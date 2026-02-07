/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#FAFAFD',
          100: '#EEEFFD',
          200: '#D5D7FA',
          300: '#A8ACF4',
          400: '#7B82EB',
          500: '#5B65DC',
          600: '#4A52C4',
          700: '#3A40A0',
          800: '#2A307C',
          900: '#122056',
        },
        brand: {
          dark: '#122056',
          main: '#5B65DC',
          light: '#EEEFFD',
          lighter: '#FAFAFD',
          white: '#FFFFFF',
        },
      },
    },
  },
  plugins: [],
}
