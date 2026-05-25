// 请求基础配置
const BASE_URL = 'http://localhost:8080' // 修改为您的后端地址

// 请求拦截器
const request = (options = {}) => {
  return new Promise((resolve, reject) => {
    // 从本地存储获取token
    const token = uni.getStorageSync('token')
    
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'token': token || ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          // 判断业务状态码
          if (res.data.code === '200') {
            resolve(res.data)
          } else if (res.data.code === '401') {
            // token失效，跳转到登录页
            uni.showToast({
              title: '登录已过期，请重新登录',
              icon: 'none'
            })
            setTimeout(() => {
              uni.reLaunch({
                url: '/pages/login/login'
              })
            }, 1500)
            reject(res.data)
          } else {
            uni.showToast({
              title: res.data.msg || '请求失败',
              icon: 'none'
            })
            reject(res.data)
          }
        } else {
          uni.showToast({
            title: '网络请求失败',
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

export default request
