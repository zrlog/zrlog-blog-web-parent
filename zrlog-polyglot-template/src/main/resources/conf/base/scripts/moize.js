// path.js 的内容
'use strict';

const moize = {
    shallow: function (...args) {
        return args.filter(Boolean).join('/').replace(/\/+/g, '/');
    },
};

// 关键：必须赋值给 module.exports
module.exports = moize;