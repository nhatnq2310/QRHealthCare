import mongoose from "mongoose";
import { idTransform } from "./User.js";

const productSchema = new mongoose.Schema(
  {
    slug:             { type: String, unique: true, index: true },
    name:             { type: String, default: "" },
    price:            { type: Number, default: 0 }, // VND, integer
    oldPrice:         { type: Number, default: null },
    badge:            { type: String, default: "" },
    description:      { type: String, default: "" },
    imageUrl:         { type: String, default: "" },
    category:         { type: String, default: "" }, // sticker | card | tag
    bloodGroupSelect: { type: Boolean, default: false }, // legacy — kept for old data; new code reads emergencyContactRequired
    emergencyContactRequired: { type: Boolean, default: false }, // when true, cart UI prompts for a phone
    lowStock:         { type: Boolean, default: false },
    quantity:         { type: String, default: "" },
    dimensions:       { type: String, default: "" },
    materials:        { type: String, default: "" },
    durability:       { type: [String], default: [] },
    shipping:         { type: String, default: "" },
  },
  { toJSON: idTransform }
);

export default mongoose.model("Product", productSchema);
