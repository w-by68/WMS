import { mergeConfig } from 'vite';
import eslint from 'vite-plugin-eslint';
import baseConfig from './vite.config.base';

export default mergeConfig(
  {
    mode: 'development',
    server: {
      port: 5173,
      open: true,
      fs: {
        strict: true,
      },
      // 后端未配置 CORS（TODO(08-运维)：由后端统一处理跨域），
      // 开发期用代理把 /api 转发到 8080，前端只需请求同源 /api
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    plugins: [
      eslint({
        cache: false,
        // 仅提示不阻断：lint 报错不应导致 dev server 无法启动
        failOnError: false,
        include: ['src/**/*.ts', 'src/**/*.tsx', 'src/**/*.vue'],
        exclude: ['node_modules'],
      }),
    ],
  },
  baseConfig
);
