/**
 * 本地存储封装
 */

/**
 * 设置存储
 * @param {String} key 
 * @param {Any} value 
 */
export function setStorage(key, value) {
  try {
    uni.setStorageSync(key, value)
    return true
  } catch (e) {
    console.error('setStorage error:', e)
    return false
  }
}

/**
 * 获取存储
 * @param {String} key 
 * @returns {Any}
 */
export function getStorage(key) {
  try {
    return uni.getStorageSync(key)
  } catch (e) {
    console.error('getStorage error:', e)
    return null
  }
}

/**
 * 移除存储
 * @param {String} key 
 */
export function removeStorage(key) {
  try {
    uni.removeStorageSync(key)
    return true
  } catch (e) {
    console.error('removeStorage error:', e)
    return false
  }
}

/**
 * 清空存储
 */
export function clearStorage() {
  try {
    uni.clearStorageSync()
    return true
  } catch (e) {
    console.error('clearStorage error:', e)
    return false
  }
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return getStorage('userInfo') || {}
}

/**
 * 设置用户信息
 */
export function setUserInfo(userInfo) {
  return setStorage('userInfo', userInfo)
}

/**
 * 获取 Token
 */
export function getToken() {
  return getStorage('token') || ''
}

/**
 * 设置 Token
 */
export function setToken(token) {
  return setStorage('token', token)
}

/**
 * 清除登录信息
 */
export function clearLoginInfo() {
  removeStorage('token')
  removeStorage('userInfo')
}
