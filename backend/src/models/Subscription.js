import mongoose from "mongoose";
import { idTransform } from "./User.js";

// One history entry per lifecycle event, used to build the admin
// subscribers/cancellations report (week/month/year/lifetime).
const historyEntry = new mongoose.Schema(
  {
    event:         { type: String, enum: ["trial_started", "subscribed", "renewed", "cancelled", "expired"], required: true },
    plan:          { type: String, default: "" },   // "trial" | "monthly" | "flexible" | "yearly"
    amount:        { type: Number, default: 0 },
    extraProfiles: { type: Number, default: 0 },
    at:            { type: Number, default: () => Date.now() },
  },
  { _id: false }
);

/**
 * "Gói duy trì lưu trữ hồ sơ" — the profile-storage maintenance subscription.
 * One document per user. Lifecycle:
 *   trial (30 days, free, starts at first profile creation)
 *     -> active (paid: monthly / flexible / yearly)
 *     -> expired (missed payment -> profiles get frozen/privatized)
 *     -> cancelled (user cancelled -> profiles frozen immediately)
 * Status is recomputed lazily (see routes/subscriptions.js) rather than via a
 * cron job — every read checks whether periodEnd has passed.
 */
const subscriptionSchema = new mongoose.Schema(
  {
    userId:         { type: String, required: true, unique: true, index: true },
    status:         { type: String, enum: ["trial", "active", "expired", "cancelled"], default: "trial" },
    plan:           { type: String, enum: ["trial", "monthly", "flexible", "yearly"], default: "trial" },
    extraProfiles:  { type: Number, default: 0 }, // slots bought beyond the base 5, via the flexible plan
    periodStart:    { type: Number, default: () => Date.now() },
    periodEnd:      { type: Number, default: () => Date.now() + 30 * 24 * 60 * 60 * 1000 }, // trial: 30 days
    lastAmount:     { type: Number, default: 0 },
    paymentRef:     { type: String, default: "" },
    history:        { type: [historyEntry], default: [] },
    createdAt:      { type: Number, default: () => Date.now() },
    updatedAt:      { type: Number, default: () => Date.now() },
  },
  { toJSON: idTransform }
);

export default mongoose.model("Subscription", subscriptionSchema);
