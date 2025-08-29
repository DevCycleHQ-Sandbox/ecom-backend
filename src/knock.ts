import * as http from "http"
import * as https from "https"
import * as url from "url"

// Default configuration
const defaultUrl = "http://134.209.65.225:3002"
const minInterval = 500 // milliseconds
const maxInterval = 5000 // milliseconds

// Parse command line arguments
const args = process.argv.slice(2)
const baseUrl = args[0] || defaultUrl

// Parse the URL to determine if we need HTTP or HTTPS
const parsedUrl = url.parse(baseUrl)
const client = parsedUrl.protocol === "https:" ? https : http

// Store JWTs for users
const userJWTs = new Map<string, string>()

let count = 0

console.log(
  `Authenticating users and knocking on ${baseUrl}/api/products with randomized intervals (${minInterval}ms - ${maxInterval}ms). Press Ctrl+C to stop.`
)

// Function to make POST request for authentication
function authenticate(username: string, password: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const loginData = JSON.stringify({ username, password })

    const options = {
      hostname: parsedUrl.hostname,
      port: parsedUrl.port || (parsedUrl.protocol === "https:" ? 443 : 80),
      path: "/api/auth/login",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(loginData),
      },
    }

    const req = client.request(options, (res: any) => {
      let data = ""

      res.on("data", (chunk: any) => {
        data += chunk
      })

      res.on("end", () => {
        try {
          const response = JSON.parse(data)
          if (res.statusCode === 200 && response.token) {
            resolve(response.token)
          } else {
            reject(
              new Error(
                `Authentication failed for ${username}: Status ${res.statusCode}`
              )
            )
          }
        } catch (error) {
          reject(
            new Error(`Failed to parse auth response for ${username}: ${error}`)
          )
        }
      })
    })

    req.on("error", (error: any) => {
      reject(new Error(`Auth request failed for ${username}: ${error.message}`))
    })

    req.write(loginData)
    req.end()
  })
}

// Function to get random interval between min and max
function getRandomInterval(): number {
  return (
    Math.floor(Math.random() * (maxInterval - minInterval + 1)) + minInterval
  )
}

// Function to get random JWT from available users
function getRandomJWT(): string | null {
  const jwts = Array.from(userJWTs.values())
  if (jwts.length === 0) return null
  return jwts[Math.floor(Math.random() * jwts.length)]
}

// Function to get random username for logging
function getRandomUsername(): string {
  const usernames = Array.from(userJWTs.keys())
  if (usernames.length === 0) return "unknown"
  return usernames[Math.floor(Math.random() * usernames.length)]
}

// Function to make the request to /api/products
function knock() {
  count++
  const startTime = Date.now()
  const jwt = getRandomJWT()
  const username = getRandomUsername()

  if (!jwt) {
    console.log(`Knock ${count}: No JWT available, skipping...`)
    scheduleNextKnock()
    return
  }

  const options = {
    hostname: parsedUrl.hostname,
    port: parsedUrl.port || (parsedUrl.protocol === "https:" ? 443 : 80),
    path: "/api/products",
    method: "GET",
    headers: {
      Authorization: `Bearer ${jwt}`,
    },
  }

  const req = client.request(options, (res: any) => {
    const duration = (Date.now() - startTime) / 1000
    console.log(
      `Knock ${count} (${username}): Status ${
        res.statusCode
      } in ${duration.toFixed(4)}s`
    )
  })

  req.on("error", (error: any) => {
    console.log(`Knock ${count} (${username}): Failed - ${error.message}`)
  })

  req.end()

  scheduleNextKnock()
}

// Function to schedule the next knock with random interval
function scheduleNextKnock() {
  const nextInterval = getRandomInterval()
  setTimeout(knock, nextInterval)
}

// Initialize authentication for both users
async function initialize() {
  try {
    console.log("Authenticating users...")

    // Authenticate both users
    const [userJWT, adminJWT] = await Promise.all([
      authenticate("user", "password"),
      authenticate("admin", "password"),
    ])

    // Store JWTs in map
    userJWTs.set("user", userJWT)
    userJWTs.set("admin", adminJWT)

    console.log("Authentication successful for both users!")
    console.log("Starting randomized knocking...")

    // Start knocking
    knock()
  } catch (error) {
    console.error("Failed to initialize:", error)
    process.exit(1)
  }
}

// Start the process
initialize()
