var jsWrapperLoader = {
    async: false,

    // 【终极解药】：必须显式告诉 min.js，什么叫相对路径！
    isRelative: function (filename) {
        // 只要是以 ./ 或 ../ 开头，就返回 true
        return javaLoader.isRelative(filename);
    },

    resolve: function (from, to) {
        // 只要上面返回 true，min.js 就会乖乖调用这里！
        return javaLoader.resolve(from, to);
    },

    getSource: function (name) {
        return javaLoader.getSource(name);
    }
};

var env = new nunjucks.Environment(jsWrapperLoader, {autoescape: false});