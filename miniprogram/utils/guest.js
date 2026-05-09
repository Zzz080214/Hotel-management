const GUEST_KEY = "profile_guest_list";

function defaultGuests() {
  return [
    {
      id: "guest-default-1",
      name: "张同学",
      phone: "13800138000",
      idCard: "440101199901011234",
      isDefault: true
    }
  ];
}

function readGuestList(key = GUEST_KEY, fallback = defaultGuests) {
  try {
    const stored = wx.getStorageSync(key);
    return Array.isArray(stored) && stored.length ? stored : fallback();
  } catch (error) {
    return fallback();
  }
}

function writeGuestList(list, key = GUEST_KEY) {
  try {
    wx.setStorageSync(key, list);
  } catch (error) {
    return;
  }
}

function normalizeIdCard(value) {
  return String(value || "").replace(/\s/g, "").toUpperCase().slice(0, 18);
}

function isValidIdCard(value) {
  return /^\d{17}[\dX]$/.test(normalizeIdCard(value));
}

function getPreferredGuest(list) {
  const guests = Array.isArray(list) ? list.filter(Boolean) : [];
  if (!guests.length) {
    return null;
  }

  const defaultValidGuest = guests.find((item) => item.isDefault && isValidIdCard(item.idCard));
  if (defaultValidGuest) {
    return defaultValidGuest;
  }

  const anyValidGuest = guests.find((item) => isValidIdCard(item.idCard));
  if (anyValidGuest) {
    return anyValidGuest;
  }

  const defaultGuest = guests.find((item) => item.isDefault);
  if (defaultGuest) {
    return defaultGuest;
  }

  return guests[0];
}

module.exports = {
  GUEST_KEY,
  defaultGuests,
  readGuestList,
  writeGuestList,
  normalizeIdCard,
  isValidIdCard,
  getPreferredGuest
};
