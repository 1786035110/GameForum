export default {
    state: () => ({
      darkMode: localStorage.getItem('darkMode') === 'true' || false
    }),
    mutations: {
      toggleDarkMode(state) {
        state.darkMode = !state.darkMode
        localStorage.setItem('darkMode', state.darkMode)
        // 应用深色模式到HTML元素
        if (state.darkMode) {
          document.documentElement.classList.add('dark-mode')
        } else {
          document.documentElement.classList.remove('dark-mode')
        }
      }
    },
    actions: {
      initializeTheme({ state }) {
        if (state.darkMode) {
          document.documentElement.classList.add('dark-mode')
        }
      }
    }
  }