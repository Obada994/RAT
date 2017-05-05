# RAT
## What is it?
RAT is a Remote Adminstration Tool written completely using pure Java and using TCP sockets to connect the clients.
#### It provides the following functionalities:
- File/Folder transfer
- Terminal access (Currently supports Windows OS and Linux with the possibility to execute SUDO commands)
**The connection is encrypted using AES 128 bit encryption**
## Basic commands:
```
get "Absolute path"
cmd "command"
```
use `get "/home/uname/Desktop/folder"` or  
`get "/home/uname/Desktop/file.extension"` to get any folder or file, notice that you don't have to specify the extension of the file

use `cmd "AnyTerminalCommand"` to execute commands on the client platform
**beware that you need to use the quotation marks in both cmd & get**.
## RFC 
The RFC is quite simple and it's using [JSON](https://github.com/stleary/JSON-java).
To send a file send a JSON object with the following keys
```
{
type:   file
data: "File_Data_Encoded_As_Base64String"
file_name: fileName
file_extension: exe/txt/zip..etc
}
```
To get a file send

```
{
type:   request
data: get
path: "Abs_Path"
}
```
To execute a command send

```
{
type:   request
data: cmd
command: "Any_Vaild_Command"
}
```
**Please notice that all the objects coming in and out any client are encrypted with AES 128 bit and in order to receive or send you need to decrypt with the same key as in the other client**
