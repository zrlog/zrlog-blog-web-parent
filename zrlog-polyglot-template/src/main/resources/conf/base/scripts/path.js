// path.js 的内容
'use strict';

const path = {
    join: function (...args) {
        return args.filter(Boolean).join('/').replace(/\/+/g, '/');
    },
    extname: function (p) {
        const idx = p.lastIndexOf('.');
        return idx < 0 ? '' : p.substring(idx);
    },
    sep: '/'
};

// 关键：必须赋值给 module.exports
module.exports = path;