# LifeAdmin frontend demo

LifeAdmin is a frontend-only personal administration workflow demo. Provider listings, prices, reviews, availability, sample events and confirmations are fictional. The app does not connect to a backend, calendar or provider.

## Run locally

```sh
npm install
npm run dev
```

Open the local URL printed by Vite. Create a production build with `npm run build`.

## Try the booking flow

On Overview, choose **Find a salon for braids**, or paste a request into the AI Workspace. Compare provider matches and sample slots, review provider details from Discover, select a provider and time, then review the proposal. Approval records your decision. A separate action creates a local demo confirmation and reserves that sample slot for later runs.

The proposal shows calendar status as unavailable because no calendar is connected. Sample events on Overview are examples only and are not used for conflict checks.

## Provider search and task chat

In **Discover**, search by provider, service, or category; filter by location and budget; and clear filters. In **AI Workspace**, send with Enter or use Shift+Enter for a new line. **New conversation** clears the visible conversation while keeping saved task and booking data.

The salon, home cleaning, and tailoring examples use deterministic demo rules. To preview the search failure and recovery state, submit a message containing `[demo search error]`, then use **Retry**. The same task is retried without adding a duplicate history entry.

## Reset demo data

Open **Settings → Demo data → Reset demo data**. Tasks, local proposals, approvals, bookings and reserved sample slots are stored in browser local storage.

## Integration boundary

Mock records live in `src/data.js`. The demo adapter, validation, provider search, workflow operations and local persistence live in `src/service.js`. The chat and discovery controls are in `src/TaskChat.jsx` and `src/ProviderDiscovery.jsx`; app navigation and workflow are in `src/App.jsx`.

Set `VITE_API_BASE_URL` in a local `.env` only after backend contracts are confirmed. No API endpoints or credentials are assumed. See [FRONTEND_INTEGRATION.md](./FRONTEND_INTEGRATION.md) for the frontend operation contracts and backend questions. This project uses JavaScript and JSX with no TypeScript toolchain.
