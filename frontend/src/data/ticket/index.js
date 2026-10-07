import users from './users/operations.json' with { type: 'json' }
import activities from './activities/operations.json' with { type: 'json' }
import registration from './registration/operations.json' with { type: 'json' }
import tickets from './tickets/operations.json' with { type: 'json' }
export default [...users, ...activities, ...registration, ...tickets]
