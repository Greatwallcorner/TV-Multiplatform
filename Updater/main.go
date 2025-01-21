package main

import (
	"flag"
	"fmt"
	"os"
)

/**
启动程序的时候不能放在需要更新的程序运行目录下面
*/

func main() {
	var path = flag.String("path", "", "program run path")
	var file = flag.String("file", "", "zip file path")
	flag.Parse()
	_, err := os.Stat(*path)
	if os.IsNotExist(err) {
		fmt.Printf("path is not exist %s", *path)
		return
	}
	_, err = os.Stat(*file)
	if os.IsNotExist(err) {
		fmt.Printf("zip file is not exist %s", *file)
		return
	}
	executable, err := os.Executable()
	if err != nil {
		fmt.Printf("get executable path err:%s", executable)
		return
	}
	os.RemoveAll(*path)

}
