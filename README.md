# backery recipes

# TO DO
- fix project structure to accommodate for minified templates and pages

# DESCRIPTION

إفطار سُهيل هو موقع لنشر طرق تحضير وتقديم الطعام مع ارشاد صوتي ومؤقت

# Techniology
## Cookies
so much I would like to document
## Json Web Token (JWT)
In my words, it is a data oriented tool for authentication. I mainly use it in combination with refresh token to reduce database requests. [here](https://github.com/hnasr/javascript_playground/tree/master/jwt-course) is a repository by hussein nasser.
# SOFTWARE
## java
to run uber file / jar file run `java -jar jarfile`
## Clojure
to build the directory run `clojure -T:build uber`
## clojurescript
the build tool I choose is shadow-cljs. Installed using the command `npm install --save-dev shadow-cljs` but could get the same result with `npm add shadow-cljs -D`.
for development run `npx shadow-cljs watch <BUILD-ID>` where BUILD-ID is define in `shadow-cljs.edn`. As an example `npx shadow-cljs watch register`

## typescript
STATUS: TESTING
initialized with the command `npx tsc --init`. Compile with `npx tsc`

## shadcn/ui
shadcn is componentless library for react. it requires typescript.
- initilized with `npm shadcn-ui@latest init`
- add component with `npx shadcn-ui@latest add` or just copy it to `src/ts`

## tailwindcss
we can compile css directly with tailwindcss with the command `npx tailwindcss -i resources/styles/input.css -o public/css/output.css --watch` and if we replace the watch flag with `--minify` tailwindcss will output a minified file.
## postcss
Fortunately we can also watch multiple files at the same time with postcss-cli. If we store styles in `resources/styles` and output css to `public/css` then the watch command is:
`npx postcss-cli resources/styles --dir public/css --watch`
## Browser-sync
for live server reloading with basic html and css. Install with `npm install -g browser-sync`. Run with `browser-sync start --server --files 'public/*'` if you want a static server. In case you are using the backend run `browser-sync start --proxy "localhost:3000" --files "public/**/*" "resources/**/*" "src/**/*"`
## clojuredart
used to build android and ios app with flutter, see [clojuredart](https://github.com/Tensegritics/ClojureDart/tree/main)
inialize with `clj -M:cljd init`.
## start emulator
Run `flutter emulators --launch Pixel_XL_API_34` from root directory of the project. To list emulators run `flutter emulators` 
## start clojuredart watcher
In root directory of project run `clj -M:cljd flutter`

# LESSONS
## Oauth or 'Sign in with Github' button
I'm following the instructions in [Github Docs](https://docs.github.com/en/apps/creating-github-apps/writing-code-for-a-github-app/building-a-login-with-github-button-with-a-github-app), [google dev](https://developers.google.com/identity/openid-connect/openid-connect) and [oauth2](https://www.oauth.com/oauth2-servers/definitions/) to build a login button. I realized that for Oauth you should Have a `client_id` so that your users can send a request to Github (or any identity provider) to give permission to my website, then Github sends me a `code` to a specified url that I determine. This `code` is then used to get an access token for my website to issue requests on behalf of the user. Github need a `client_secret` in order to verify that `client_id` used to request `code` matches.

It finally clicked with me that Github api can work with JWT+refresh token in this manner: we start by user authorizing the website to get an access token to github. This redirects to a specified path in the website with a code. the server sends the code to github an get an access token which will serve to get data about the user, mainly email address and [nick]name[^1]. There are two possibilities from here, either user has been registered or is a new user. We go by this by checking the emails in the database: 
1. user doesn't exist in database.
   the server creates a record for the user then associates a refresh token to the user, containing an identifier (ID for example). Finally the server returns the refresh token to user and a JWT token with the same data. The JWT toekn could be stored in cookies since it is required in every request. However, refresh token is only required every so often ( determined by configuration ) which leads us to store it in browser localStorage.
2. user exists in database.
   there is no need to creat a new record. Therefore, the server returns with refresh token and JWT token.

There should also be extra care to handle the case where user does exist in a database but as a temporary user[^2]. Meaning, the user didn't sign in with github ( or any authorization server implemented in the future) but can access an account via JWT+refresh token.

## sending data to browser
I thought of several option, first to simply send another request after receiving the page. The tradeoff is of course the extra overhead of tcp and http request. Another solution is to embed the data in html document. [MDN](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script#embedding_data_in_html) suggests a script tag with json. For this solution to work I had to send data in a json format and specify that it is safe for templating library I use [selmer](https://github.com/yogthos/Selmer#json). A slight modification to the last solution is to embed the data in a div tag. This has the advantage of not relying on the safe option but need to hide the div with display:hidden

Conclusion, I choose the mdn solution because it is semantically correct and doesn't have vulnerabilities

## sending data from browser
the [fetch api](https://devdocs.io/dom/fetch_api/using_fetch) is great for handling http requests. You can upload json, file and even [abort request](https://devdocs.io/dom/abortcontroller).

## redirect in javascript
a great answer on [stackoverflow](https://stackoverflow.com/questions/4744751/how-do-i-redirect-with-javascript). Here is the summary: there are three means to achieve redirects:
1. `window.location="newUrl"` will redirect to url and save currect page in browser history.
2. `window.location.replace="newUrl"` will redirect to url but will not keep currect page in session history, simulating an HTTP redirect.
3. `window.location.href="newUrl"` simulate clicking an `a` (anchor) tag. Same behavior as replace.

window object can be omitted since it is global.

## save to clipboard in javascript
from the runes of [w3school](https://www.w3schools.com/howto/howto_js_copy_clipboard.asp) we can see the navigator object is used to save to clipboard. here is an example:

``` javascript
let text="text I want to be clipped";
navigator.clipboard.writeText(text);
console.log("text in clipboard!!");
```

## if statement in selmer templating library
I thought of using a data attribute to indicate if an image should appear or not in recipe.html but decided to go with another route instead.
Because It bugged me how testing equality is in the library I thought it would be helpful to document it for future:
`data-active="{% if recipe.id = 1 %}true{% else %}false{% endif %}"`
testing equality is with one equal sign like in clojure instead of two equal signs like in python. It was counter intuitive because selmer is based on django.

## list properties of javascript object
there are two way in javascript, first with `Object.keys(obj)` and second with `for(let prop in obj){...}`. In Clojurescript we can convert the first option to the form `(js/Object.keys obj)` and second solution is from [stackoverflow](https://stackoverflow.com/questions/31143674/how-to-list-the-properties-and-functions-of-a-javascript-object-in-clojurescript#31224012) `(js-keys carouselslide)`
## minify static files
Since I use tailwindcss it handles minifing css files and same for clojurescript and shadow-cljs. As for html, I opted for [asset-minifier](https://github.com/yogthos/asset-minifier/blob/master/src/asset_minifier/core.clj) but unfortunately it is undocumented. Here are the info for minifying html. To use `asset-minifier.core/minify-html` we provide source directory, target directory and **mandatory** options. options are same for [html-compressor/compressor](https://github.com/alehatsman/clj-html-compressor#config-options-with-default-values). for example: simple map `{:enabled true}`

## generate jwt secret
from [stackoverflow](https://stackoverflow.com/questions/31309759/what-is-secret-key-for-jwt-based-authentication-and-how-to-generate-it) I got the command `openssl rand -base64 32`. See also [auth0](https://auth0.com/blog/brute-forcing-hs256-is-possible-the-importance-of-using-strong-keys-to-sign-jwts)
## ON CONFLICT in postgresql and sqlite
from [sqlite.org](https://www.sqlite.org/lang_upsert.html), postgresql requires fully qualified reference in the update statement that follows on conflict. The example from sqlite main website:

> CREATE TABLE vocabulary(word TEXT PRIMARY KEY, count INT DEFAULT 1);
> INSERT INTO vocabulary(word) VALUES('jovial')
>   ON CONFLICT(word) DO UPDATE SET count=count+1;

should be written as:

> CREATE TABLE vocabulary(word TEXT PRIMARY KEY, count INT DEFAULT 1);
> INSERT INTO vocabulary(word) VALUES('jovial')
>   ON CONFLICT(word) DO UPDATE SET count=vocabulary.count+1;

## Deref Clojure Vars
A simple way to make you code [repl-friendly](https://clojure.org/guides/repl/enhancing_your_repl_workflow) is to derefrence the vars instead of supplying its value. Let `a` be a variable with value `1`. if we define a function that referenced `a` for example `(+ a 4)` then when reading the function `a` will be replaced with `1`. On the other hand, `@#'a` will be read as `(deref (var a))`. There is a video on youtube but I can't seem to find it.
## Generate KeyStore
to use http2 I stumbled upon [ring-jetty9-adapter](https://github.com/sunng87/ring-jetty9-adapter) and had to generate a .jks file. [oracle](https://docs.oracle.com/cd/E19509-01/820-3503/ggfen/index.html) has a good tutorial how to generate it. The main tool is [keytool](https://docs.oracle.com/javase/10/tools/keytool.htm#JSWOR-GUID-5990A2E4-78E3-47B7-AE75-6D1826259549). keystore password `1234client` and file is in `resources/clientkeystore.jks`. There is also this [answer](https://stackoverflow.com/questions/47434877/how-to-generate-keystore-and-truststore) and this <https://unix.stackexchange.com/questions/347116/how-to-create-keystore-and-truststore-using-self-signed-certificate>. <https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html> <https://docs.oracle.com/cd/E19798-01/821-1751/ghlgv/index.html> <https://www.sslshopper.com/article-most-common-java-keytool-keystore-commands.html> <https://shadow-cljs.github.io/docs/UsersGuide.html#_ssl>
### Generate Self Signed Certificate
	openssl req -x509 -newkey rsa:4096 -nodes -sha256 -subj '/CN=localhost' -keyout localhost-private.pem -out localhost-cert.pem
# Copyright 2023 Salim Khatib

I would like to publish the project under [AGPL](https://www.gnu.org/licenses/agpl-3.0.html) or a modification of it or [GPL](https://www.gnu.org/licenses/gpl-3.0.txt) but it is incompatible with [clojure[script]](https://clojure.org/community/license) [license](https://www.eclipse.org/org/documents/epl-1.0/EPL-1.0.txt). If anyone knows a way around, please contact me. 
# LICENSE
Distributed under the Eclipse Public License, the same as [Clojure](https://clojure.org/community/license).

[^1]: Github API does have a refresh token we can store in order to update current data on users in a set interval by a background job. Maybe even a cron job.
[^2]: Does the word ephemeral work here? or volatile?
