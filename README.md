# RAT
## What is it?
RAT is a Remote Adminstration Tool written completely using pure Java.
#### It provides the following functionalities:
- File/Folder transfer
- Terminal access (Currently supports Windows os and Linux with the possibility to execute SUDO commands)
**The connection is encrypted using AES 128 bit encryption**
## Basic commands:
```
get "Absolute path"
cmd "command"
```
use `get "/home/uname/Desktop/folder"` or  
`get "/home/uname/Desktop/file.extension"` to get any folder or file, notice that you don't have to specify the extension of the file

use `cmd "AnyTerminalCommand"` to execute commands on the client platform
**notice:** you need to use the quotation marks in both cmd & get.
## RFC 

It's been built using [JSON](https://github.com/stleary/JSON-java)
