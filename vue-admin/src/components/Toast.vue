<template>
  <div class="toast" :class="{ show: adminState.toastVisible }">
    {{ adminState.toastMessage }}
  </div>
</template>

<script setup>
import { adminState } from '../stores/admin.js'
</script>
