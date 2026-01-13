package main

import (
	"archive/zip"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
)

func main() {
	var path = flag.String("path", "", "program run path")
	var file = flag.String("file", "", "zip file path")
	flag.Parse()

	if *path == "" {
		fmt.Println("path is required")
		return
	}
	if *file == "" {
		fmt.Println("file is required")
		return
	}

	_, err := os.Stat(*path)
	if os.IsNotExist(err) {
		fmt.Printf("path is not exist %s\n", *path)
		return
	}
	_, err = os.Stat(*file)
	if os.IsNotExist(err) {
		fmt.Printf("zip file is not exist %s\n", *file)
		return
	}

	executable, err := os.Executable()
	if err != nil {
		fmt.Printf("get executable path err:%v\n", err)
		return
	}

	fmt.Printf("Starting update...\n")
	fmt.Printf("Program path: %s\n", *path)
	fmt.Printf("Zip file: %s\n", *file)
	fmt.Printf("Updater path: %s\n", executable)

	backupPath := *path + ".backup"
	fmt.Printf("Backup path: %s\n", backupPath)

	if err := backupDirectory(*path, backupPath); err != nil {
		fmt.Printf("Backup failed: %v\n", err)
		return
	}

	fmt.Println("Backup completed")

	if err := removeAllFiles(*path, executable); err != nil {
		fmt.Printf("Remove files failed: %v\n", err)
		restoreBackup(backupPath, *path)
		return
	}

	fmt.Println("Old files removed")

	if err := extractZip(*file, *path); err != nil {
		fmt.Printf("Extract failed: %v\n", err)
		restoreBackup(backupPath, *path)
		return
	}

	fmt.Println("Extract completed")

	if err := os.RemoveAll(backupPath); err != nil {
		fmt.Printf("Remove backup failed: %v\n", err)
	}

	fmt.Println("Backup removed")

	if err := startProgram(*path); err != nil {
		fmt.Printf("Start program failed: %v\n", err)
		return
	}

	fmt.Println("Update completed successfully")
}

func backupDirectory(src, dst string) error {
	return filepath.Walk(src, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		relPath, err := filepath.Rel(src, path)
		if err != nil {
			return err
		}

		dstPath := filepath.Join(dst, relPath)

		if info.IsDir() {
			return os.MkdirAll(dstPath, info.Mode())
		}

		return copyFile(path, dstPath)
	})
}

func copyFile(src, dst string) error {
	srcFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer srcFile.Close()

	dstFile, err := os.OpenFile(dst, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0644)
	if err != nil {
		return err
	}
	defer dstFile.Close()

	_, err = io.Copy(dstFile, srcFile)
	return err
}

func removeAllFiles(dir, excludePath string) error {
	return filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if path == dir {
			return nil
		}

		if strings.EqualFold(path, excludePath) {
			return nil
		}

		if info.IsDir() {
			return os.RemoveAll(path)
		}

		return os.Remove(path)
	})
}

func extractZip(zipPath, dest string) error {
	r, err := zip.OpenReader(zipPath)
	if err != nil {
		return err
	}
	defer r.Close()

	for _, f := range r.File {
		fpath := filepath.Join(dest, f.Name)

		if f.FileInfo().IsDir() {
			os.MkdirAll(fpath, f.Mode())
			continue
		}

		if err := os.MkdirAll(filepath.Dir(fpath), 0755); err != nil {
			return err
		}

		outFile, err := os.OpenFile(fpath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, f.Mode())
		if err != nil {
			return err
		}

		rc, err := f.Open()
		if err != nil {
			outFile.Close()
			return err
		}

		_, err = io.Copy(outFile, rc)
		rc.Close()
		outFile.Close()

		if err != nil {
			return err
		}
	}

	return nil
}

func restoreBackup(backupPath, destPath string) error {
	fmt.Println("Restoring backup...")
	if err := os.RemoveAll(destPath); err != nil {
		return err
	}
	return os.Rename(backupPath, destPath)
}

func startProgram(path string) error {
	var exeName string
	if runtime.GOOS == "windows" {
		exeName = "LumenTV.exe"
	} else {
		exeName = "LumenTV"
	}

	exePath := filepath.Join(path, exeName)

	cmd := &exec.Cmd{}
	if runtime.GOOS == "windows" {
		cmd.Path = exePath
		cmd.Args = []string{exePath}
	} else {
		cmd.Path = exePath
		cmd.Args = []string{exePath}
	}

	cmd.Dir = path

	return cmd.Start()
}
