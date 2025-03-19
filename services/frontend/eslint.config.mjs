// @ts-check

import eslint from "@eslint/js";
import eslintPluginPrettierRecommended from "eslint-plugin-prettier/recommended";
import tseslint from "typescript-eslint";

export default tseslint.config(
    eslint.configs.recommended,
    ...tseslint.configs.strictTypeChecked,
    ...tseslint.configs.stylisticTypeChecked,
    eslintPluginPrettierRecommended,
    {
        ignores: ["build/", "node_modules/", "reports/", "scripts/", "coverage/"],
    },
    {
        languageOptions: {
            parserOptions: {
                projectService: true,
                tsconfigRootDir: import.meta.dirname,
            },
        },
        plugins: { },
    },
    {
        rules: {
            "no-unused-vars": "off",
            "@typescript-eslint/no-unused-vars": "warn",
            "react/react-in-jsx-scope": "off",
            "prettier/prettier": "warn",
            "multiline-comment-style": ["warn", "starred-block"],
        },
    },
    { files: ["**/*.mjs"], ...tseslint.configs.disableTypeChecked },
);
