// Configure loading modules from the lib directory,
// except for 'app' ones, which are in a sibling
// directory.
requirejs.config({
    baseUrl: 'lib',
    shim: {
      'colorbrewer': {
        exports: 'colorbrewer'
      }
    },
    paths: {
        app: '../app'
    }
});

requirejs(['app/main']);
