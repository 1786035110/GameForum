import { createStore } from 'vuex'
import createPersistedState from 'vuex-persistedstate'
import user from './modules/user'
import theme from './modules/theme'
import friends from './modules/friends'
import chat from './modules/chat'

const store = createStore({
  modules: {
    user,
    theme,
    friends,
    chat
  },
  plugins: [
    createPersistedState({
      key: 'snake-game',
      paths: ['user.isLoggedIn', 'user.userInfo', 'user.token']
    })
  ]
})

export default store