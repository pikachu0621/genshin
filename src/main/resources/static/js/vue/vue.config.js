module.exports = {
    devServer: {
        port: 8080,
        host: "0.0.0.0",
        https: false,  // 协议
        open: true,  // false 启动服务时自动打开浏览器访问
        proxy: {
            '/admin': {
                target: "http://admin.caorulai.com",
                changOrigin: true,  // 是否要代理
                pathRewrite: {
                    '^/admin': '/'
                }
            }
        }
    },
    publicPath: "./",
    outputDir: "dist",
    assetsDir: "static",
    indexPath: "index.html"
}
