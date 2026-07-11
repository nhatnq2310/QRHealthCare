import mongoose from "mongoose";
import { idTransform } from "./User.js";

// _id: false on subdocs so they don't get noise ObjectIds we don't need.
const emergencyContact   = new mongoose.Schema({ name: String, phone: String, relationship: String }, { _id: false });
const allergy            = new mongoose.Schema({ name: String, severity: String, reaction: String }, { _id: false });
const medication         = new mongoose.Schema({ name: String, dosage: String, frequency: String }, { _id: false });
const medicalCondition   = new mongoose.Schema({ name: String, diagnosedDate: String, notes: String }, { _id: false });
const profileAddress     = new mongoose.Schema({ street: String, ward: String, district: String, city: String, country: { type: String, default: "Việt Nam" } }, { _id: false });
const insurance          = new mongoose.Schema({ provider: String, policyNumber: String, expiryDate: String }, { _id: false });

const profileSchema = new mongoose.Schema(
  {
    userId:             { type: String, required: true, index: true },
    profileType:        { type: String, enum: ["human", "pet"], default: "human" },
    fullName:           { type: String, default: "" },
    gender:             { type: String, default: "" },
    birthDate:          { type: String, default: "" },
    bloodGroup:         { type: String, default: "" },
    height:             { type: String, default: "" },
    weight:             { type: String, default: "" },
    hairColor:          { type: String, default: "" },
    eyeColor:           { type: String, default: "" },
    identificationMark: { type: String, default: "" },
    personalNumber:     { type: String, default: "" },
    organDonor:         { type: Boolean, default: false },
    showOrganDonor:     { type: Boolean, default: true },
    isPrivate:          { type: Boolean, default: false },
    // Forced private by the subscription system (unpaid/expired maintenance
    // plan) — independent of the user's own isPrivate preference. Both flags
    // OR together in public-profile.js's privacy check.
    subscriptionFrozen: { type: Boolean, default: false },
    // ── Family notification (subscription perk) ─────────────────────────────
    // A registered family member's device gets a push notification every time
    // this profile's QR is scanned (only while the owner's subscription is
    // active), plus a link that grants them full profile visibility
    // regardless of the isPrivate/subscriptionFrozen state — via familyAccessToken.
    familyFcmTokens:    { type: [String], default: [] },
    familyAccessToken:  { type: String, default: "" }, // generated lazily on first family registration
    hiddenFields:       { type: [String], default: [] },
    emergencyContacts:  { type: [emergencyContact], default: [] },
    allergies:          { type: [allergy], default: [] },
    medications:        { type: [medication], default: [] },
    medicalConditions:  { type: [medicalCondition], default: [] },
    addresses:          { type: [profileAddress], default: [] },
    healthInsurance:    { type: [insurance], default: [] },  // bảo hiểm y tế
    lifeInsurance:      { type: [insurance], default: [] },  // bảo hiểm nhân thọ
    healthDocuments:    { type: [String], default: [] }, // relative URLs from POST /uploads
    notes:              { type: String, default: "" },   // free-form user notes (dietary plan, etc.)
    viewCount:          { type: Number, default: 0 },
    createdAt:          { type: Number, default: () => Date.now() },
  },
  { toJSON: idTransform }
);

export default mongoose.model("Profile", profileSchema);
