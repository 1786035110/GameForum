import { createStore } from 'vuex'
import createPersistedState from 'vuex-persistedstate'
import theme from './modules/theme'
import friends from './modules/friends'
import user from './modules/user'

export default createStore({
  modules: {
    theme,
    friends,
    user
  },
  plugins: [
    createPersistedState({
      key: 'snake-game',
      paths: ['user.isLoggedIn', 'user.userInfo']
    })
  ]
})