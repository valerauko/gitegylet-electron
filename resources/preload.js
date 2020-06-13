// TODO: make this cljs too.
// currently i can't get it to work because of error:
// Error: async hook stack has become corrupted (actual: 17, expected: 0)
const { ipcRenderer } = require('electron')

process.once('loaded', () => {
  // forwards window message to ipcMain
  window.addEventListener('message', evt => {
    if (evt.data.type === 'ipc-request') {
      ipcRenderer.send('ipc-request', evt.data.payload)
    }
  })

  // forwards message from ipcMain to window
  ipcRenderer.on('ipc-response', (event, payload) => {
    window.postMessage({ type: 'ipc-response', payload: payload })
  })
})
