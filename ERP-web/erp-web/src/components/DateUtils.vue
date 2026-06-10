<template>
  <div class="clock-card">
    <p class="clock-label">当前时间</p>
    <p class="clock-date">{{ nowTime }}</p>
    <div class="clock-digits" aria-live="polite">
      <span class="clock-unit">{{ time.hour }}</span>
      <span class="split">:</span>
      <span class="clock-unit">{{ time.minitus }}</span>
      <span class="split">:</span>
      <span class="clock-unit clock-unit--sec">{{ time.seconds }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DateUtils',
  props: ['s'],
  data() {
    return {
      time: {
        hour: '',
        minitus: '',
        seconds: ''
      },
      nowTime: '',
      week: ['星期天', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    }
  },
  mounted() {
    this.dateTime()
  },
  methods: {
    dateTime() {
      this.timeFormate()
      setTimeout(() => {
        this.dateTime()
      }, 1000)
    },
    timeFormate() {
      const newtime = new Date()
      this.time.hour = this.getIncrease(newtime.getHours(), 2)
      this.time.minitus = this.getIncrease(newtime.getMinutes(), 2)
      this.time.seconds = this.getIncrease(newtime.getSeconds(), 2)
      this.nowTime =
        this.getIncrease(newtime.getFullYear(), 4) +
        '年' +
        this.getIncrease(newtime.getMonth() + 1, 2) +
        '月' +
        this.getIncrease(newtime.getDate(), 2) +
        '日 · ' +
        this.week[newtime.getDay()]
    },
    getIncrease(num, digit) {
      var increase = ''
      for (var i = 0; i < digit; i++) {
        increase += '0'
      }
      return (increase + num).slice(-digit)
    }
  }
}
</script>

<style scoped>
.clock-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 100%;
}

.clock-label {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.clock-date {
  margin: 0 0 16px;
  font-size: 15px;
  color: #334155;
  font-weight: 500;
}

.clock-digits {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-variant-numeric: tabular-nums;
}

.clock-unit {
  font-size: 42px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1;
  color: #0f172a;
  background: linear-gradient(180deg, #f8fafc, #e2e8f0);
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.clock-unit--sec {
  font-size: 32px;
  color: #475569;
}

.split {
  font-size: 36px;
  font-weight: 300;
  color: #94a3b8;
  animation: blink 1s step-end infinite;
  padding: 0 2px;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.2;
  }
}
</style>
