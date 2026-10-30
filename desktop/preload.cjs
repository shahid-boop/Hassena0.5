const { contextBridge } = require("electron");

// Intentionally expose no filesystem, shell, or arbitrary IPC access to the page.
contextBridge.exposeInMainWorld("jarvisDesktop", Object.freeze({
  platform: process.platform,
  appName: "JARVIS 22"
}));
