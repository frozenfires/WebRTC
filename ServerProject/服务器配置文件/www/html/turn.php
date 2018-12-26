 <?php  
            $request_username = $_GET["username"];  
            if(empty($request_username)) {  
                echo "username == null";  
                exit;  
            }  
            $request_key = $_GET["key"];  
            $time_to_live = 600;  
            $timestamp = time() + $time_to_live;//失效时间  
            $response_username = $timestamp.":".$_GET["username"];  
            $response_key = $request_key;  
            if(empty($response_key))  
            $response_key = "inesadt"; //constants.py中CEOD_KEY  

            $response_password = getSignature($response_username, $response_key);  

            $jsonObj = new Response();  
            $jsonObj->username = $response_username;  
            $jsonObj->password = $response_password;  
            $jsonObj->ttl = 86400;
            //此处需配置自己的服务器
            $jsonObj->uris= array("stun:119.23.39.109:3478","turn:119.23.39.109:3478?transport=udp[inesadt:inesadt]","turn:119.23.39.109:3478?transport=tcp[inesadt:inesadt]");

            echo json_encode($jsonObj);  

        /**   
         * 使用HMAC-SHA1算法生成签名值   
         *   
         * @param $str 源串   
         * @param $key 密钥   
         *   
         * @return 签名值   
         */
        function getSignature($str, $key) {
        $signature = "";
        if (function_exists('hash_hmac')) {
        $signature = base64_encode(hash_hmac("sha1", $str, $key, true));
        } else {
        $blocksize = 64;
        $hashfunc = 'sha1';
        if (strlen($key) > $blocksize) {
        $key = pack('H*', $hashfunc($key));
        }
        $key = str_pad($key, $blocksize, chr(0x00));
        $ipad = str_repeat(chr(0x36), $blocksize);
        $opad = str_repeat(chr(0x5c), $blocksize);
        $hmac = pack(
        'H*', $hashfunc(
        ($key ^ $opad) . pack(
        'H*', $hashfunc(
        ($key ^ $ipad) . $str
        )
        )
        )
        );
        $signature = base64_encode($hmac);
        }
            return $signature;
            }

            class Response {  
                public $username = "";  
                public $password = "";  
                public $ttl = "";  
                public $uris = array("");  
            }  

        ?>



