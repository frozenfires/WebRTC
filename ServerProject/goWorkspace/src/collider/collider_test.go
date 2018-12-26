// Copyright (c) 2014 The WebRTC project authors. All Rights Reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file in the root of the source
// tree.

package collider

import (
	"golang.org/x/net/websocket"
	"encoding/json"
	"flag"
	"fmt"
	"net"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"
)

var serverAddr string
var once sync.Once
var registerTimeout = time.Second
var cl *Collider

var port = flag.Int("port", 8089, "The port that Collider listens to")

func startCollider() {
	serverAddr = "localhost:" + strconv.Itoa(*port)

	cl = &Collider{
		roomTable: newRoomTable(registerTimeout, "http://"+serverAddr),
		dash:      newDashboard(),
	}

	go cl.Run(*port, false)
	fmt.Println("Test WebSocket server listening on ", serverAddr)
}

func newConfig(t *testing.T, path string) *websocket.Config {
	wsaddr := fmt.Sprintf("ws://%s%s", serverAddr, path)
	lh := "http://localhost"
	c, err := websocket.NewConfig(wsaddr, lh)
	if err != nil {
		t.Fatalf("NewConfig(%q, %q) got error: %s, want nil", wsaddr, lh, err.Error())
	}
	return c
}

func setup() {
	once.Do(startCollider)
	cl.roomTable = newRoomTable(registerTimeout, "http://"+serverAddr)
}

func addWsClient(t *testing.T, roomID string, clientID string) *websocket.Conn {
	c, err := net.Dial("tcp", serverAddr)
	if err != nil {
		t.Fatalf("net.Dial(tcp, %q) got error: %s, want nil", serverAddr, err.Error())
	}
	config := newConfig(t, "/ws")
	conn, err := websocket.NewClient(config, c)
	if err != nil {
		t.Fatalf("websocket.NewClient(%v, %v) got error: %s, want nil", config, c, err.Error())
	}

	// Registers the client.
	m := wsClientMsg{
		Cmd:      "register",
		ClientID: clientID,
		RoomID:   roomID,
	}
	write(t, conn, m)

	return conn
}

func read(t *testing.T, conn *websocket.Conn) string {
	var data = make([]byte, 512)
	n, err := conn.Read(data)
	if err != nil {
		t.Errorf("conn.Read(%v) got error: %v, want nil", data, err)
	}
	return string(data[0:n])
}

func write(t *testing.T, conn *websocket.Conn, data interface{}) {
	enc := json.NewEncoder(conn)
	err := enc.Encode(data)
	if err != nil {
		t.Errorf("json.NewEncoder(%v).Encode(%v) got error: %v, want nil", conn, data, err)
	}
}

func postSend(t *testing.T, roomID string, clientID string, msg string) {
	urlstr := "http://" + serverAddr + "/" + roomID + "/" + clientID
	r := strings.NewReader(msg)
	_, err := http.Post(urlstr, "application/octet-stream", r)
	if err != nil {
		t.Errorf("http.Post(%q, %q) got error: %q, want nil", urlstr, msg, err)
	}
}

func postDel(t *testing.T, roomID string, clientID string) {
	var c http.Client
	urlstr := "http://" + serverAddr + "/" + roomID + "/" + clientID
	req, err := http.NewRequest("DELETE", urlstr, nil)
	if err != nil {
		t.Errorf("http.NewRequest(DELETE, %q, nil) got error: %v, want nil", urlstr, err)
	}
	_, err = c.Do(req)
	if err != nil {
		t.Errorf("http.Client.Do(%v) got error: %v", req, err)
	}
}

func expectConnectionClose(t *testing.T, conn *websocket.Conn) {
	var m string
	err := websocket.Message.Receive(conn, &m)
	if err == nil || err.Error() != "EOF" {
		t.Errorf("websocket.Message.Receive(%v) = %v, want EOF", conn, err)
	}
}

func expectReceiveMessage(t *testing.T, conn *websocket.Conn, msg string) {
	var m wsClientMsg
	err := json.Unmarshal([]byte(read(t, conn)), &m)

	if err != nil {
		t.Errorf("json.Unmarshal([]byte(read(t, conn))) got error: %v, want nil", err)
	}
	if m.Msg != msg {
		t.Errorf("After json.Unmarshal([]byte(read(t, conn)), &m), m.Msg = %s, want %s", m.Msg, msg)
	}
}

func expectReceiveError(t *testing.T, conn *websocket.Conn) {
	var m wsServerMsg
	if err := json.Unmarshal([]byte(read(t, conn)), &m); err != nil {
		t.Errorf("json.Unmarshal([]byte(read(t, conn)), &m) got error: %v, want nil", err)
	}
	if m.Error == "" {
		t.Errorf("After json.Unmarshal([]byte(read(t, conn)), &m), m.Error = %v, want non-empty", m.Error)
	}
}

func waitForCondition(f func() bool) bool {
	for i := 0; i < 10 && !f(); i++ {
		time.Sleep(1000)
	}
	return f()
}

func TestWsForwardServer(t *testing.T) {
	setup()
	c1 := addWsClient(t, "abc", "123")
	c2 := addWsClient(t, "abc", "456")

	// Sends a message from conn1 to conn2.
	m := wsClientMsg{
		Cmd: "send",
		Msg: "hello",
	}
	write(t, c1, m)
	expectReceiveMessage(t, c2, m.Msg)
	c1.Close()
	c2.Close()
}

// Tests that an error is returned if the same client id is registered twice.
func TestWsForwardServerDuplicatedID(t *testing.T) {
	setup()
	c := addWsClient(t, "abc", "123")

	// Registers the same client again.
	m := wsClientMsg{
		Cmd:      "register",
		ClientID: "123",
		RoomID:   "abc",
	}
	write(t, c, m)
	expectReceiveError(t, c)
	expectConnectionClose(t, c)
}

// Tests that an error is returned if the same client tries to register a second time.
func TestWsForwardServerConnectTwice(t *testing.T) {
	setup()
	c := addWsClient(t, "abc", "123")

	// Registers again.
	m := wsClientMsg{
		Cmd:      "register",
		ClientID: "123",
		RoomID:   "abc",
	}
	write(t, c, m)
	expectReceiveError(t, c)
	expectConnectionClose(t, c)
}

// Tests that message sent through POST is received.
func TestHttpHandlerSend(t *testing.T) {
	setup()
	c := addWsClient(t, "abc", "123")

	// Sends a POST request and expects to receive the message on the websocket connection.
	m := "hello!"
	postSend(t, "abc", "456", m)
	expectReceiveMessage(t, c, m)
	c.Close()
}

// Tests that message cached through POST is delivered.
func TestHttpHandlerSendCached(t *testing.T) {
	setup()

	// Sends a POST request and expects to receive the message on the websocket connection.
	m := "hello!"
	rid, src, dest := "abc", "456", "123"
	postSend(t, rid, src, m)
	if !waitForCondition(func() bool { return cl.roomTable.rooms[rid] != nil }) {
		t.Errorf("After a POST request to the room %q, cl.roomTable.rooms[%q] = nil, want non-nil", rid, rid)
	}

	c := addWsClient(t, rid, dest)
	expectReceiveMessage(t, c, m)
	if !waitForCondition(func() bool { return len(cl.roomTable.rooms[rid].clients[src].msgs) == 0 }) {
		t.Errorf("After a POST request from the room %q from client %q and registering client %q, cl.roomTable.rooms[%q].clients[%q].msgs = %v, want emtpy", rid, src, dest, rid, src, cl.roomTable.rooms[rid].clients[src].msgs)
	}

	c.Close()
}

// Tests that deleting the client through DELETE works.
func TestHttpHandlerDeleteConnection(t *testing.T) {
	setup()
	rid, cid := "abc", "1"
	c := addWsClient(t, rid, cid)

	// Waits until the server has registered the client.
	if !waitForCondition(func() bool { return cl.roomTable.rooms[rid] != nil }) {
		t.Errorf("After registering client %q in room %q, cl.roomTable.rooms[%q] = nil, want non-nil", cid, rid, rid)
	}

	// Deletes the client.
	postDel(t, rid, cid)
	expectConnectionClose(t, c)
	if !waitForCondition(func() bool { return len(cl.roomTable.rooms) == 0 }) {
		t.Errorf("After deleting client %q from room %q, cl.roomTable.rooms = %v, want empty", cid, rid, cl.roomTable.rooms)
	}
}

func TestRoomCleanedUpAfterTimeout(t *testing.T) {
	setup()

	// Sends a POST request to create a new and unregistered client.
	r, c := "abc", "1"
	postSend(t, r, c, "hi")
	if !waitForCondition(func() bool { return cl.roomTable.rooms[r] != nil }) {
		t.Errorf("After a POST request to the room %q, cl.roomTable.rooms[%q] = nil, want non-nil", r, r)
	}
	time.Sleep(registerTimeout + time.Second)

	if l := len(cl.roomTable.rooms); l != 0 {
		t.Errorf("After timeout without registering the new client, len(cl.roomTable.rooms) = %d, want 0", l)
	}
}

func TestDeregisteredClientNotRemovedUntilTimeout(t *testing.T) {
	setup()

	rid, cid := "abc", "1"
	conn := addWsClient(t, rid, cid)
	c, _ := cl.roomTable.room(rid).client(cid)

	conn.Close()

	// Waits for the client to deregister.
	if !waitForCondition(func() bool { return !c.registered() }) {
		t.Errorf("After websockt.Connection.Close(), client.registered() = true, want false")
	}

	// Checks that the client is still in the room.
	if actual, _ := cl.roomTable.room(rid).client(cid); actual != c {
		t.Errorf("After websockt.Connection.Close(), cl.roomTable.room[rid].client[cid] = %v, want %v", actual, c)
	}

	// Checks that the client and room are removed after the timeout.
	time.Sleep(registerTimeout + time.Second)
	if l := len(cl.roomTable.rooms); l != 0 {
		t.Errorf("After timeout without re-registering the new client, len(cl.roomTable.rooms) = %d, want 0", l)
	}
}

func TestReregisterClientBeforeTimeout(t *testing.T) {
	setup()

	rid, cid := "abc", "1"
	conn := addWsClient(t, rid, cid)
	c, _ := cl.roomTable.room(rid).client(cid)

	conn.Close()

	// Waits for the client to deregister.
	if !waitForCondition(func() bool { return !c.registered() }) {
		t.Errorf("After websockt.Connection.Close(), client.registered() = true, want false")
	}

	// Checks that the client is still in the room.
	if actual, _ := cl.roomTable.room(rid).client(cid); actual != c {
		t.Errorf("After websockt.Connection.Close(), cl.roomTable.room[rid].client[cid] = %v, want %v", actual, c)
	}

	// Reregister the client.
	conn = addWsClient(t, rid, cid)

	// Waits for the client to be registered.
	if !waitForCondition(func() bool { return c.registered() }) {
		t.Errorf("After addWsClient(...) again, client.registered() = false, want true")
	}

	// Checks that the timer has been stopped.
	if c.timer != nil {
		t.Errorf("After addWsClient() again, client.timer = %v, want nil", c.timer)
	}
}
