/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./public/**/*.{html,js}"],
  theme: {
    screens: {
      sm: '480px',
      md: '768px',
      lg: '976px',
      xl: '1440px',
    },
    extend: {
      fontFamily: {
        amiri: "'Amiri', serif",
        NotoNaskhArabic: "'Noto Naskh Arabic', serif"
      },
      colors: {
        veryDarkBlue: 'hsl(233, 12%, 13%)',
        veryLightGray: '#F2F2F2',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    // require('tailwindcss-dir')(),
  ],
}
