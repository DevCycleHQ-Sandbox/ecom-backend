import typescriptEslint from "@typescript-eslint/eslint-plugin"
import typescriptParser from "@typescript-eslint/parser"
import prettierConfig from "eslint-config-prettier"

export default [
  {
    files: ["**/*.ts"],
    languageOptions: {
      parser: typescriptParser,
      parserOptions: {
        ecmaVersion: 2022,
        sourceType: "module",
        project: "./tsconfig.json",
      },
    },
    plugins: {
      "@typescript-eslint": typescriptEslint,
    },
    rules: {
      // TypeScript rules
      "@typescript-eslint/no-explicit-any": "warn",
      "@typescript-eslint/no-unused-vars": "error",
      "@typescript-eslint/no-var-requires": "error",
      "@typescript-eslint/explicit-function-return-type": "off",
      "@typescript-eslint/explicit-module-boundary-types": "off",
      "@typescript-eslint/no-empty-function": "warn",

      // General JavaScript rules
      "no-console": "off", // Allow console in backend
      "no-debugger": "error",
      "no-duplicate-imports": "error",
      "no-unused-vars": "off", // Turned off in favor of @typescript-eslint/no-unused-vars
      "prefer-const": "error",
      "no-var": "error",
      eqeqeq: "error",
      curly: "error",

      // Node.js specific rules
      "no-process-exit": "error",
      "no-path-concat": "error",

      // Import rules
      "sort-imports": [
        "error",
        {
          ignoreCase: true,
          ignoreDeclarationSort: true,
        },
      ],
    },
  },
  prettierConfig,
]
