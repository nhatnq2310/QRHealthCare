import mongoose from "mongoose";
import { idTransform } from "./User.js";

const qrTagSchema = new mongoose.Schema(
  {
    tagCode:     { type: String, required: true, unique: true, uppercase: true, index: true },
    pin:         { type: String, required: true },
    profileId:   { type: String, default: null, index: true },
    productType: { type: String, default: "" },
    orderId:     { type: String, default: "" },
    scanCount:   { type: Number, default: 0 },
    createdAt:   { type: Number, default: () => Date.now() },
  },
  { toJSON: idTransform }
);

export default mongoose.model("QrTag", qrTagSchema);
