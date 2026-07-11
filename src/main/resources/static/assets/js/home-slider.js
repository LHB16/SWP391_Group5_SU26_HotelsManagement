/**
 * home-slider.js
 * Infinite circular card slider -- transform-based, no scroll jerk.
 *
 * The script wraps .slider-container in a .slider-clip div (overflow:hidden)
 * so that the track is clipped WITHOUT hiding the prev/next buttons.
 *
 * Layout after init:
 *   .slider-wrapper   (overflow: visible, position: relative)
 *     button.prev-btn  (absolute, left: -22px)
 *     .slider-clip     (overflow: hidden, clips the track)
 *       .slider-container  [lastN clones | originals | firstN clones]
 *                           driven by transform: translateX
 *     button.next-btn  (absolute, right: -22px)
 */
(function () {
  'use strict';

  function initSlider(wrapper) {
    const container = wrapper.querySelector('.slider-container');
    const prevBtn   = wrapper.querySelector('.prev-btn');
    const nextBtn   = wrapper.querySelector('.next-btn');
    if (!container || !prevBtn || !nextBtn) return;

    const originals = Array.from(container.children);
    const n = originals.length;
    if (n < 2) return;

    // 1. Wrap container in a clip div so track is hidden but buttons are NOT
    const clip = document.createElement('div');
    clip.className = 'slider-clip';
    clip.style.cssText = 'overflow:hidden;width:100%;position:relative;';
    wrapper.insertBefore(clip, container);
    clip.appendChild(container);

    // 2. Build triple track: [lastN clones][originals][firstN clones]
    [...originals].reverse().forEach(c =>
      container.insertBefore(c.cloneNode(true), container.firstChild));
    originals.forEach(c => container.appendChild(c.cloneNode(true)));

    // 3. Container layout
    container.style.overflow   = 'visible';
    container.style.flexShrink = '0';
    container.style.willChange = 'transform';

    // 4. Helpers
    const GAP = 20;
    const getCardW = () => {
      const el = container.firstElementChild;
      return el ? el.offsetWidth + GAP : 280;
    };

    let idx  = n;
    let busy = false;

    const moveTo = (newIdx, animate) => {
      const cw = getCardW();
      container.style.transition = animate
        ? 'transform 0.38s cubic-bezier(0.4,0,0.2,1)'
        : 'none';
      container.style.transform = 'translateX(' + (-newIdx * cw) + 'px)';
      idx = newIdx;
    };

    // 5. Init: silently jump to the real cards (idx = n)
    requestAnimationFrame(function() {
      requestAnimationFrame(function() { moveTo(n, false); });
    });

    // 6. Slide
    const slide = function(dir) {
      if (busy) return;
      busy = true;
      moveTo(idx + dir, true);
      setTimeout(function() {
        var adjusted = idx;
        if (idx >= n * 2) adjusted = idx - n;
        if (idx <  n)     adjusted = idx + n;
        if (adjusted !== idx) moveTo(adjusted, false);
        requestAnimationFrame(function() { busy = false; });
      }, 400);
    };

    nextBtn.addEventListener('click', function() { slide(+1); });
    prevBtn.addEventListener('click', function() { slide(-1); });
  }

  function run() {
    document.querySelectorAll('.slider-wrapper').forEach(initSlider);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', run);
  } else {
    run();
  }
})();
