module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('karma-junit-reporter'),
      
    ],
    client: {
      jasmine: {
      },
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/thrash-buddy'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcov', subdir: 'lcov' },
        { type: 'cobertura', subdir: '.', file: 'coverage.xml' }
      ]
    },
    reporters: ['progress', 'kjhtml', 'junit'],
    browsers: ['Chrome'],
    restartOnFileChange: true
  });
};
