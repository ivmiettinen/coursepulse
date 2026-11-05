// Only check staged frontend files. Fixes are an explicit developer action.
export default {
  '*.{ts,html}': 'eslint --max-warnings=0',
};
