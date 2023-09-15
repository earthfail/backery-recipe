/** @type {import('tailwindcss').Config} */
let content = ["./src/**/*.{clj,cljs}","./resources/**/*.{html,js}","./public/*.{html,js}"]
//if(process.env.PROJ_ENV === 'dev'){
//    console.log("dev");
//    content.push("./dev/resources/templates/**/*.{html,js}");
//    content.push("./dev/resources/html/*.{html,js}");
//}else{
//    console.log("not dev" + process.env.PROJ_ENV);
//    content.push("./resources/templates/**/*.{html,js}");
//    content.push("./public/*.{html,js}");
//}
module.exports = {
	    content: content,
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
	    aspectRatio:{
		'phone': '3/5',
		'desk': '4/3',
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
		grass: '#16AC16',
		oil: '#EAE9E1',
		pastel: '#637E93',
		pastel2: '#787B97'
	    },
	    keyframes: {
		"accordion-down": {
		    from: { height: 0 },
		    to: { height: "var(--radix-accordion-content-height)" },
		},
		"accordion-up": {
		    from: { height: "var(--radix-accordion-content-height)" },
		    to: { height: 0 },
		},
	    },
	    animation: {
		"accordion-down": "accordion-down 0.2s ease-out",
		"accordion-up": "accordion-up 0.2s ease-out",
	    },
	},
    },
    plugins: [
	require('@tailwindcss/forms'),
	require("tailwindcss-animate"),
	// require('tailwindcss-dir')(),
    ],
}
