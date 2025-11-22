# Full-stack web application

The purpose of this project is to provide an example of a simple web application with a back-end server and a front-end
client.

## Project Structure

The project is structured as follows:

- `client`: This directory contains the React.js client application code, as well as scripts to build a .apk file using
  Cordova and to create a `gh-pages`-ready bundle. The Cordova build script will demand a fair amount of non-versioned
  configuration.
- `server`: This directory contains the Java Spring Boot server code as well as Docker configuration including a
  Postgres with pg-vector image, Postman tests and Bash and Python scripts to deploy via SSH and other tasks.

Every connection between these components is made with security policies in place. Secrets sit at .env files (templates
versioned).  
The set-up is monolith-oriented to provide economic (or even free) means for on-line usage. The database used to be
decoupled from the server, but now is part of the same Docker-compose. It is technically possible to ignore or remove
the dockerized database and configure connection to some other, although certain assumptions are made at the server's
code.

## Programs

### Sudoku Application

If you click `Generate`, you will then be able to select `Generate` to generate a randomly generated, one-solution
Sudoku, where the number of clues will be 17 at difficulty 9 and 6 per difficulty level less. You will also be able to
click `Fetch` to pull a randomly selected, previously imported Sudoku from the database. you can also press `enter` or
`ctrl+enter` respectively.  
Once a sudoku is showing you will be able to navigate it with the `arrow keys` or with the mouse. You cannot remove or
override clues (marked with a grey background). Selecting `solve` will solve the Sudoku immediately and render it
uneditable. Selecting `check` will have the Sudoku blink red if at least one number is wrong, and blink green otherwise.
You can also press `ctrl+enter` or `enter` respectively.  
Placing the last number in the sudoku will make an auto-check.  
Pressing `escape` or clicking `reset` at any time will go back to "the main menu".  
It takes the program less than <s>2</s> 0.1 milliseconds on average to solve a Sudoku  
If you click `Choose file` you will be able to import sudoku from a `.txt` file, where each sudoku has a one-line
header (which will be ignored) and then one nine-digit line per row:

    # Sudoku Puzzle 1
    530070000
    600195000
    098000060
    800060003
    400803001
    700020006
    060000280
    000419005
    000080079
    # Sudoku Puzzle 2
    003020600
    900305001
    001806400
    008102900
    700000008
    006708200
    002609500
    800203009
    005010300

### Calculator Application

Does what you would imagine it does, with the twist that typing something will store it in the database, and can be
retrieved with the `ans` button.  
There's no session control, so the value stored behind the `ans` button is always the same.

### Secret Santa Application

Type the name of three or more players (cannot be left empty and has to be unique) so when clicking `Start raffle` the
program creates a single cycle with participants, where each ought to gift the next one.  
The program will then prompt each participant (in input order) to click the `Press me to show!` button, after which it
will show their giftee's name for 2 seconds.  
To clarify, the idea is to use this app ideally on a phone, passing it around so each participant can intimately check
who they have to gift.

### Rubik Timer

While the timer is very precise, the displayed time's refresh rate is declared as a constant and used to save computing
power.  
Select a puzzle for which to generate a random scramble.  
Set the timer to zero by holding `spacebar` or touching your phone screen (buttons and form fields have priority over
this functionality).  
Start the timer by releasing, or cancel the start-up by dragging your finger or pressing any other key. The scramble
won't be lost.  
Stats will be updated on the fly. Press `show more stats` to access detailed statistics and the times log.  
Pressing a statistic will show the times involved. Pressing a time from the global list will allow you to delete it or
mark it as `DNF` (Did Not Finish).  
It is also possible to edit the last time by double tapping on mobile or pressing `Del` on desktop.  
The app is provided with logic to improve UX like input control (like preventing from clicking a button right after
stopping the timer), screen lock prevention or orientation-based dynamic rendering.  
As with the other apps, pressing `escape` is equivalent to clicking or pressing `reset`.

### Shopping Application

The entry-point is a login form that generates an HTTPS-only cookie and sends it back to the front end upon correct
authentication.  
From there on the cookie is used for authorization and in data queries it parametrizes a where clause.  
The filter and quantity inputs use a debouncer to handle user input.  
The delete button only deletes the record for this user. If no record exists for this product the product record is also
deleted.  
the `X` button is just a quicker way to set to zero. Product edition is only allowed to authorized users. Although
clickable by any, since authorization is server-handled.  
Categories don't sort by name but by `order`. This is so to create categories in an order that matches my usual
supermarket.  
Products also contain an `order` property, so no alphabetic sorting possible (unless it happens to be the defined
one).  
The `hide zero` button is a global filter that filters out rows with 0 as quantity, and the best functionality of the
app, from a real world POV.

### Trading Application

Its front-end provides data visualization for hard-coded companies combining last close (or real time) market data and
position relative to it and AI RAG-based buy, hold and sell recommendations as well as the possibility to generate such
recommendations on-demand.  
These recommendations are based on current per-user portfolio state relative to each company, previous market data with
KPIs (EMA, MACD, RSI, ATR, OBV, out-of-hours gap) and recent, company-related news (a Finnbert Python sentiment
generator for such news exists, although it is currently not configured).  
Detailed information about each recommendation is accessible by clicking `BUY`, `HOLD`, or `SELL` on the interface (news
used for generation and AI model rationale).
It is also possible to update the per-user portfolio with buy and sell orders, including a commission to calculate gross
and net unrealized gains and losses.
The app works with three configured scheduled tasks:

- A midnight task that retrieves close data for the day, using TwelveData's API or Finnhub's API.
- A morning task that generates recommendations right before the US markets open using Yahoo API for on-the-fly news,
  the previously mentioned sentiment generator and different AI services, combined with rate limiters and concurrent
  execution to avoid rate limits.
- Two morning tasks executed later in the morning capture a tick of all listed companies, to later infer program
  success (See [these artificial tests](server/src/test/java/com/lucas/server/ManualRunTest.java)), that include also
  on-close and some-days-after inference.

## Installation and Usage

To run this application, follow these steps:

1. Clone the repository to your local machine
2. Install Java, Node.js and Docker if not already installed
3. Configure the necessary `.env` files
4. Start the application server and client. The client will deploy on port 3000 simply running `npm run start`. The
   server needs explicit access to the .env file, since I'm not using any tool for automatic injection. The database
   will start next to the server on port 5432, as well as an `adminer` on port 8081, thanks to
   `spring-boot-docker-compose` configuration.

## Deploy

The next are notes to myself in case I forget my own workflows.

### Deploy front-end

- Commit the changes somewhere (if they exist in `master` branch that's fine, else maybe push to remote, maybe create a
  temporary branch from `develop`)
- Revert branch to commit before deploy commit (which should be the last)
- Cherry-pick new changes
- Verify possible sensitive information in `.env` and source files
- run `npm run build`
- Commit and push changes in a `deploy`-named commit. The GitHub pipeline will do the rest
- The idea is to not clutter the git history with changes over bundled files, that's why it is very important to <u>make
  sure the "deploy" commit only contains build script-generated changes</u>

### Deploy back-end

- For a server + database container setup, there's simply a `server/deploy.sh` script. Otherwise...
- Disable spring-boot-docker-compose
- Configure database connection in `application-prod.yaml`. Unfortunately, the server depends on pg-vector. Besides that
  it is dialect-agnostic
- Flyway is configured to automatically create a server-compatible schema keeping `ddl-auto` as `none`
- Another option is to rely on Hibernate to create the schema (i.e. ddl-auto create / update), disabling Flyway
  altogether
- It is also always possible to connect to the database server through HTTP or SSL or some client like VSCode extensions
  or Adminer and run seeding scripts
- Compile using `.\mvnw package -DskipTests` from /server dir
- Upload `server.jar` with FTP or SSH. The target machine will need to load the necessary environment variables
- Start web server.

### Package Android installer

- in `master` branch, edit the code as needed, i.e. exclude components from the root component (`App.js`)
- Run `client/build-android.sh` which receives two optional arguments
    - The first one is the name of the package, so it should probably be some camel-cased name like `calculator`
    - The second one is the type of bundle to generate (debug, ready to upload to the marketplace, not sure if others)
- The script will output a `.apk` and point to it in the logs
