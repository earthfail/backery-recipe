# DESCRIPTION
share interactive recipe with timers.

# INFRASTRUCTURE
## BACKEND


## FRONTEND



--- TEMP ---
routes I want to implement:
1. / is introduction about the business and services
2. /api/v1/:order-id an endpoint for generating a page to take order and to submit order for example:
   2.1 GET request responds with:
	   - Name of company to deliver order
	   - Name of customer that issued order
	   - list of available time slots
	   + [SECURITY] a unique identifier for the request
   2.2 POST request must:
	   + the unique identifier from the GET request
	   - order-id
	   - selected time slots
3. /sign-up , /login endpoints to register a company and pay subscription
4. /dashboard/:company-id  inspects order status
   + a good feature to implement is to have near real time status. Meaning update order status without the need to refresh. *Could be accomplished by server sent events (seen in pedestal) or websockets (still unclear)
   5. /profile responds with information about user


to help read json from postgres with next.jdbc here is a link of instruction 
https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.847/doc/getting-started/tips-tricks#working-with-json-and-jsonb
