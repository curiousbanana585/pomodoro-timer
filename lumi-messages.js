/*
  LUMI'S EXTRA MESSAGES
  ------------------------------------------------------------------
  Add your own lines for Lumi's speech bubble here. Whatever you add is
  mixed in at random with the built-in lines, so every now and then you'll
  get one of yours. Nothing here is required: delete a line, leave a list
  empty ([]), or delete this whole file and LOAF still works.

  Quick rules
    - Each list is a set of quoted lines separated by commas.
    - Keep lines short (a few words fit best on a phone).
    - Use straight quotes "like this". If a line contains a quote or
      apostrophe, wrap it in the other kind of quote: "Haz's on a roll".
    - You can drop in these placeholders:
        {n}     the pomodoro number (1, 2, 3...)
        {of}    how many pomodoros make a set (the "long break every" number)
        {mins}  the length of the current bit, e.g. "5 min"
    - After editing, refresh the page (on an installed copy, close and
      reopen the app once or twice so it picks up the new file).

  Set replace to true if you want ONLY your lines for a list that has some
  (the built-in ones for that list are then skipped). Leave it false to mix.
*/
window.LOAF_MESSAGES = {
  replace: false,

  // the moment a focus session starts
  focusStart: [
    // "Pomodoro {n}. Haz mode: on.",
  ],

  // every minute or so during focus (Lumi is loafing under the timer)
  focus: [
    // "Eyes on the prize. The prize is a biscuit.",
  ],

  // the last minute of a focus session
  focusLastMin: [
    // "Sixty seconds! Nose to the finish line.",
  ],

  // during a short break (Lumi is doing zoomies)
  short: [
    // "Kettle on!",
  ],

  // during a long break (Lumi is asleep)
  long: [
    // "Snack o'clock.",
  ],

  // time's up, waiting for a tap to start the next focus
  waitFocus: [
    // "Ready for round {n}?",
  ],

  // time's up, waiting for a tap to start a break
  waitBreak: [
    // "Break! Go on, you've earned it.",
  ],

  // the reminders while Lumi waits, IN ORDER: 1st nudge, 2nd nudge, ... 5th nudge.
  // Lines added here come after the built-in five, so set replace: true if you
  // want your own escalation to be used.
  nudge: [
  ],

  // when you pause
  paused: [
    // "Bathroom break? Fair.",
  ],

  // when you come back after a refresh mid-session
  welcomeBack: [
    // "Oh, you're back! I didn't move. Obviously.",
  ],
};
