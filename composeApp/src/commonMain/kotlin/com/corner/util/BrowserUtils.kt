package com.corner.util

import java.awt.Desktop
import java.io.File
import java.nio.file.Files

object BrowserUtils {
    private var lastOpenTime = 0L
    fun openBrowserWithHtml(m3u8Url: String) {
        val now = System.currentTimeMillis()
        if (now - lastOpenTime < 1000) { // 1秒内只允许打开一次
            return
        }
        lastOpenTime = now
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                // 创建临时HTML文件
                val htmlContent = """
                    <!DOCTYPE html>
                    <html lang="zh-CN">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>LumenTV Compose—WebPlayer</title>
                        <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
                        <style>
                            :root {
                                --primary-color: #2196F3;
                                --secondary-color: #4CAF50;
                                --background-color: #1a1a1a;
                                --text-color: #ffffff;
                                --border-radius: 8px;
                                --input-bg-color: #333333;
                                --input-border-color: #444444;
                            }
                    
                            body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                margin: 0;
                                padding: 0;
                                background-color: var(--background-color);
                                color: var(--text-color);
                                min-height: 100vh;
                                display: flex;
                                flex-direction: column;
                            }
                    
                            .top-bar {
                                display: flex;
                                align-items: center;
                                padding: 1rem;
                                background-color: #2d2d2d;
                                gap: 1rem;
                            }
                    
                            #url-input {
                                flex: 1;
                                padding: 0.8rem;
                                border: 2px solid var(--input-border-color);
                                border-radius: var(--border-radius);
                                font-size: 1rem;
                                background-color: var(--input-bg-color);
                                color: var(--text-color);
                                transition: border-color 0.3s ease;
                            }
                    
                            #url-input:focus {
                                border-color: var(--primary-color);
                                outline: none;
                            }
                    
                            button {
                                padding: 0.8rem 1.5rem;
                                background-color: var(--secondary-color);
                                color: var(--text-color);
                                border: none;
                                border-radius: var(--border-radius);
                                cursor: pointer;
                                font-size: 1rem;
                                transition: background-color 0.3s ease, transform 0.2s ease;
                            }
                    
                            button:hover {
                                background-color: #45a049;
                                transform: translateY(-1px);
                            }
                    
                            button:active {
                                transform: translateY(0);
                            }
                    
                            #video-container {
                                flex: 1;
                                background-color: #000;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                overflow: hidden;
                                position: relative;
                            }
                    
                            video {
                                width: 100%;
                                height: 100%;
                                object-fit: contain;
                                max-height: calc(100vh - 100px);
                            }
                    
                            @media (max-width: 768px) {
                                .top-bar {
                                    flex-direction: column;
                                }
                    
                                #url-input, button {
                                    width: 100%;
                                }
                            }
                    
                            .loading {
                                position: absolute;
                                top: 50%;
                                left: 50%;
                                transform: translate(-50%, -50%);
                                color: white;
                                font-size: 1.2rem;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="top-bar">
                            <input type="text" id="url-input" placeholder="输入M3U8地址" value="${m3u8Url}">
                            <button onclick="loadVideo()">重新加载</button>
                        </div>
                        
                        <div id="video-container">
                            <div class="loading" id="loading">正在加载视频...</div>
                            <video id="video" controls autoplay></video>
                        </div>
                        
                        <script>
                            // 加载视频函数
                            function loadVideo() {
                                const m3u8Url = document.getElementById('url-input').value;
                                const video = document.getElementById('video');
                                const loading = document.getElementById('loading');
                                
                                if (!m3u8Url) {
                                    alert('请输入M3U8地址');
                                    return;
                                }
                    
                                loading.style.display = 'block';
                                video.style.display = 'none';
                    
                                // 清除之前的视频源
                                video.src = '';
                                
                                if (Hls.isSupported()) {
                                    const hls = new Hls({
                                        maxBufferLength: 30,
                                        maxMaxBufferLength: 600,
                                        maxBufferSize: 60 * 1000 * 1000,
                                        maxBufferHole: 5.0
                                    });
                                    
                                    hls.loadSource(m3u8Url);
                                    hls.attachMedia(video);
                                    
                                    hls.on(Hls.Events.MANIFEST_PARSED, function() {
                                        loading.style.display = 'none';
                                        video.style.display = 'block';
                                        video.play().catch(e => {
                                            console.log('自动播放被阻止:', e);
                                            video.controls = true;
                                        });
                                    });
                                    
                                    hls.on(Hls.Events.ERROR, function(event, data) {
                                        if (data.fatal) {
                                            loading.textContent = '加载失败: ' + (data.details || '未知错误');
                                            console.error('HLS Error:', data);
                                        }
                                    });
                                } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                                    // 原生支持HLS的浏览器（如Safari）
                                    video.src = m3u8Url;
                                    video.addEventListener('loadedmetadata', function() {
                                        loading.style.display = 'none';
                                        video.style.display = 'block';
                                        video.play().catch(e => {
                                            console.log('自动播放被阻止:', e);
                                            video.controls = true;
                                        });
                                    });
                                } else {
                                    loading.textContent = '您的浏览器不支持播放此视频格式';
                                }
                            }
                            
                            // 页面加载时自动加载视频
                            window.onload = function() {
                                const m3u8Url = document.getElementById('url-input').value;
                                if (m3u8Url) {
                                    loadVideo();
                                }
                            };
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                val tempDir = Files.createTempDirectory("m3u8-player").toFile()
                tempDir.deleteOnExit()

                val htmlFile = File(tempDir, "player.html").apply {
                    writeText(htmlContent)
                    deleteOnExit()
                }

                // 使用浏览器打开本地文件
                Desktop.getDesktop().browse(htmlFile.toURI())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}