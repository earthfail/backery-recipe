/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./public/**/*.{html,js,ts}","./src/main/khsalim/frontend/**/*"],
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
	  primary: '#F23636',
	  secondery: '#FDF2E7',
	  background: '#FFFAFC',
	  text: '#260214',
	  accent: '#B6610C',
	  accent2: '#E3AE7A',
	  oil: '#EAE9E1',
	  pastel: '#637E93',
	  pastel2: '#787B97'
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    // require('tailwindcss-dir')(),
],
      }
