/*
 * Copyright 2018 Naoki Seto(@Lionas)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.lionas.androidthings.sensor.mcp3008driver

import com.google.android.things.contrib.driver.pwmspeaker.Speaker

/**
 * Sound class for Sample
 * @author Naoki Seto(@Lionas)
 */
class Sound(private val speaker: Speaker) {

    companion object {
        const val C = 262
        const val CS = 278
        const val D = 294
        const val DS = 312
        const val E = 330
        const val F = 349
        const val FS = 370
        const val G = 392
        const val GS = 416
        const val A = 440
        const val AS = 467
        const val B = 494
    }

    private val sounds = listOf(C, D, E, F, G, A, B)
    private val soundsStr = listOf("C", "D", "E", "F", "G", "A", "B")

    private val soundSemitones = listOf(CS, DS, E, FS, GS, AS, B)
    private val soundSemitonesStr = listOf("C#", "D#", "E", "F#", "G#", "A#", "B")

    private var prevStep: Int = 0
    private var currentStep: Int = 0
    private var isSemitone: Boolean = false
    private var isOctave: Boolean = false

    fun play() {
        val mag = if (isOctave) 2 else 1
        if (isSemitone) {
            speaker.play(soundSemitones[currentStep].toDouble() * mag)
        } else {
            speaker.play(sounds[currentStep].toDouble() * mag)
        }
    }

    fun playOnChanged(value: Int) {
        updateKey(value)
        if (prevStep != currentStep) {
            speaker.stop()
            play()
            prevStep = currentStep
        }
    }

    fun updateSemitone(isSemitone: Boolean) {
        this.isSemitone = isSemitone
    }

    fun updateOctave(isOctave: Boolean) {
        this.isOctave = isOctave
    }

    fun updateKey(value: Int): String {
        this.currentStep = value / 170
        return getCurrentKeyString()
    }

    fun getCurrentKeyString(): String {
        return if (this.isSemitone) {
            soundSemitonesStr[currentStep]
        } else {
            soundsStr[currentStep]
        }
    }

}