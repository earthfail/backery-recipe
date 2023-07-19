# backery recipes

# DESCRIPTION

إفطار سُهيل هو موقع لنشر طرق تحضير وتقديم الطعام مع ارشاد صوتي ومؤقت

# SOFTWARE

## clojurescript
the build tool I choose is shadow-cljs. Installed using the command `npm install --save-dev shadow-cljs` but could get the same result with `npm add shadow-cljs -D`.
for development run `npx shadow-cljs watch <BUILD-ID>` where BUILD-ID is define in `shadow-cljs.edn`. As an example `npx shadow-cljs watch register`

# typescript
STATUS: not working
initialized with the command `npx tsc --init`.

# shadcn/ui

shadcn is componentless library for react. it requires typescript.
- initilized with `npm shadcn-ui@latest init`
- add component with `npx shadcn-ui@latest add`

# LESSONS

# Copyright 2023 Salim Khatib
# LICENSE
backery-recipe is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

backery-recipe is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
