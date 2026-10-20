if (typeof TextEncoder === "undefined") {
    globalThis.TextEncoder = class {
        constructor() {
        }

        encode(str) {
            const arr = new Uint8Array(str.length);
            for (let i = 0; i < str.length; i++) {
                arr[i] = str.charCodeAt(i);
            }
            return arr;
        }
    };
}

/*
var myPlugin = function() {
    return function(style) {
        // 拦截导入语句
        style.import = function(path) {
            // 这里的 path 是 @import 后的字符串
            // 调用 Java 读取内容
            var content = javaLoader.read(path);
            // 返回解析后的节点
            return stylus.parse(content);
        };
    };
};*/
