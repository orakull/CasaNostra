// Enable SPA history fallback so deep links like /workspace/:id work in dev server
config.devServer = config.devServer || {};
config.devServer.historyApiFallback = true;
