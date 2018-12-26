<?php  
            echo "Hello World!"; 
			
			  $request_username = "inesadt";  //配置成自己的turn服务器用户名
            if(empty($request_username)) {  
                echo "username == null";  
                exit;  
            }  
			
		    $request_key = "inesadt";  //配置成自己的turn服务器密码
            $time_to_live = 600;  
            $timestamp = time() + $time_to_live;//失效时间  
            $response_username = $timestamp.":".$_GET["username"];  
            $response_key = $request_key;  
			
			 if(empty($response_key))  
            $response_key = "inesadt";//constants.py中CEOD_KEY  

		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
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
                    public $lifetimeDuration = "";
                    public $iceServers = array("");
            } 
			
        ?>
