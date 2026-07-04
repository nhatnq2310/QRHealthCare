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
    // How many QR tags to generate per unit purchased of this product.
    // 0   = no QR tag at all (e.g. a refill/accessory item like "Sticker Che Phủ Thêm")
    // 1   = the normal case (a single sticker/card/tag)
    // 2+  = a combo/bundle that contains multiple trackable items (e.g. the
    //       "Combo Sticker Y Tế + Sticker Nhóm Máu" package = 2 tags per unit)
    // NOTE: category alone is NOT a reliable signal for this — a combo and a
    // plain refill sticker can share the same category but need different
    // tag counts, which is exactly the bug this field fixes.
    qrTagsPerUnit:    { type: Number, default: 1 },
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
