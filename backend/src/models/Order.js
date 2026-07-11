import mongoose from "mongoose";
import { idTransform } from "./User.js";

const orderItem = new mongoose.Schema(
  {
    productId:        String,
    productSlug:      { type: String, default: "" }, // used by "buy again" to look the product back up
    productName:      String,
    imageUrl:         { type: String, default: "" },  // snapshot of the product image at order time
    price:            Number,
    quantity:         Number,
    emergencyContact: { type: String, default: "" }, // phone number entered at add-to-cart time
  },
  { _id: false }
);

const shippingAddress = new mongoose.Schema(
  {
    fullName: { type: String, default: "" },
    phone:    { type: String, default: "" },
    address:  { type: String, default: "" },  // street address
    city:     { type: String, default: "" },
    note:     { type: String, default: "" },  // optional delivery note
  },
  { _id: false }
);

const orderSchema = new mongoose.Schema(
  {
    userId:        { type: String, required: true, index: true },
    profileId:     { type: String, default: "" },
    items:         { type: [orderItem], default: [] },
    totalAmount:   { type: Number, default: 0 },     // final amount after discount
    discountAmount:{ type: Number, default: 0 },     // amount knocked off by the coupon
    couponCode:    { type: String, default: "" },    // empty when no coupon used
    paymentRef:    { type: String, default: "" },    // QR transfer note for reconciliation (e.g. QRH12345678)
    paymentMethod: { type: String, default: "" }, // "vietqr" | "cash" | ...
    status:        { type: String, default: "pending" },
    shippingAddress: { type: shippingAddress, default: () => ({}) }, // per-order delivery details
    qrTagIds:      { type: [String], default: [] },
    isPromo:       { type: Boolean, default: false }, // true for the free-tag gift order (first paid subscription month)
    shippingFee:   { type: Number, default: 0 },       // always 0 today — no fee system exists yet, but promo orders explicitly advertise "free shipping"
    createdAt:     { type: Number, default: () => Date.now() },
  },
  { toJSON: idTransform }
);

export default mongoose.model("Order", orderSchema);
