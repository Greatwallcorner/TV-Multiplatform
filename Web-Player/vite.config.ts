import {defineConfig} from 'vite';
import solidPlugin from 'vite-plugin-solid';

export default defineConfig({
  base: "/static",
  plugins: [solidPlugin()],
  server: {
    port: 3000,
    hmr:{
      timeout: 5
    }
    // open: true
  },
  build: {
    target: 'esnext',
  },
});
