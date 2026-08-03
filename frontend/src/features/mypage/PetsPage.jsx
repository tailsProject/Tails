// 반려동물 등록, 수정, 사진 업로드, 삭제 페이지
import { useEffect, useRef, useState } from 'react';
import { getMyPets, createPet, updatePet, deletePet, uploadPetPhoto, deletePetPhoto } from './api';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Button from '../../components/Button/Button';
import { resolveImage } from '../../utils/resolveImage';
import { PawIcon, PencilIcon, TrashIcon, PlusIcon, CameraIcon } from '../../components/Icon/Icon';
import styles from './PetsPage.module.scss';

const SPECIES_PRESETS = ['강아지', '고양이'];
const EMPTY_FORM = { name: '', birthDate: '' };

function calcAge(birthDate) {
  if (!birthDate) return '';
  const birth = new Date(birthDate);
  const today = new Date();
  const totalMonths = (today.getFullYear() - birth.getFullYear()) * 12 + (today.getMonth() - birth.getMonth());
  if (totalMonths < 0) return '';
  if (totalMonths < 12) return `${totalMonths}개월`;
  const years = Math.floor(totalMonths / 12);
  const remMonths = totalMonths % 12;
  return remMonths > 0 ? `${years}살 ${remMonths}개월` : `${years}살`;
}

export default function PetsPage() {
  const { showToast } = useToast();
  const confirm = useConfirm();
  const [pets, setPets] = useState([]);
  const [formOpen, setFormOpen] = useState(false);
  const [editingPetId, setEditingPetId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [speciesChip, setSpeciesChip] = useState('');
  const [customSpecies, setCustomSpecies] = useState('');
  const [photoFile, setPhotoFile] = useState(null);
  const [photoPreview, setPhotoPreview] = useState(null);
  const [removeExistingPhoto, setRemoveExistingPhoto] = useState(false);
  const [saving, setSaving] = useState(false);
  const objectUrlRef = useRef(null);

  async function load() {
    const res = await getMyPets();
    setPets(res.data.data);
  }

  useEffect(() => {
    load();
  }, []);

  function revokePreviewIfBlob() {
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
      objectUrlRef.current = null;
    }
  }

  function resetPhotoState(initialPreview = null) {
    revokePreviewIfBlob();
    setPhotoFile(null);
    setPhotoPreview(initialPreview);
    setRemoveExistingPhoto(false);
  }

  function openCreateForm() {
    setEditingPetId(null);
    setForm(EMPTY_FORM);
    setSpeciesChip('');
    setCustomSpecies('');
    resetPhotoState(null);
    setFormOpen(true);
  }

  function openEditForm(pet) {
    setEditingPetId(pet.petId);
    setForm({ name: pet.name, birthDate: pet.birthDate ?? '' });
    if (pet.species && !SPECIES_PRESETS.includes(pet.species)) {
      setSpeciesChip('기타');
      setCustomSpecies(pet.species);
    } else {
      setSpeciesChip(pet.species ?? '');
      setCustomSpecies('');
    }
    resetPhotoState(pet.photoImg ? resolveImage(pet.photoImg) : null);
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setEditingPetId(null);
    revokePreviewIfBlob();
  }

  function handlePhotoSelect(e) {
    const file = e.target.files[0];
    e.target.value = '';
    if (!file) return;
    revokePreviewIfBlob();
    const url = URL.createObjectURL(file);
    objectUrlRef.current = url;
    setPhotoFile(file);
    setPhotoPreview(url);
    setRemoveExistingPhoto(false);
  }

  function handlePhotoClear() {
    resetPhotoState(null);
    setRemoveExistingPhoto(true);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const species = speciesChip === '기타' ? customSpecies.trim() : speciesChip;
    const payload = {
      name: form.name,
      species: species || null,
      birthDate: form.birthDate || null,
    };
    setSaving(true);
    try {
      let petId = editingPetId;
      if (editingPetId) {
        await updatePet(editingPetId, payload);
      } else {
        const res = await createPet(payload);
        petId = res.data.data;
      }
      if (photoFile) {
        await uploadPetPhoto(petId, photoFile);
      } else if (editingPetId && removeExistingPhoto) {
        await deletePetPhoto(petId);
      }
      showToast(editingPetId ? '반려동물 정보가 수정되었습니다.' : '반려동물이 등록되었습니다.', 'success');
      closeForm();
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '저장에 실패했습니다.', 'error');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(petId) {
    const ok = await confirm('반려동물 정보를 삭제하시겠습니까?');
    if (!ok) return;
    await deletePet(petId);
    load();
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <h1>반려동물</h1>
      </div>

      <ul className={styles.list}>
        {pets.map((pet) => {
          const age = calcAge(pet.birthDate);
          return (
            <li key={pet.petId} className={styles.item}>
              {pet.photoImg ? (
                <img className={styles.avatar} src={resolveImage(pet.photoImg)} alt="" />
              ) : (
                <span className={styles.avatar}>
                  <PawIcon />
                </span>
              )}
              <div className={styles.itemBody}>
                <div className={styles.nameRow}>
                  <span className={styles.name}>{pet.name}</span>
                  {age && <span className={styles.ageBadge}>{age}</span>}
                </div>
                <span className={styles.meta}>
                  {pet.species ?? '종 미상'}
                  {pet.birthDate && ` · ${pet.birthDate}`}
                </span>
              </div>
              <div className={styles.actions}>
                <button type="button" onClick={() => openEditForm(pet)} aria-label="수정">
                  <PencilIcon />
                </button>
                <button type="button" onClick={() => handleDelete(pet.petId)} aria-label="삭제">
                  <TrashIcon />
                </button>
              </div>
            </li>
          );
        })}
        {pets.length === 0 && !formOpen && (
          <div className={styles.empty}>
            <p className={styles.emptyIcon}>
              <PawIcon />
            </p>
            <p>등록된 반려동물이 없습니다.</p>
          </div>
        )}
      </ul>

      {formOpen ? (
        <div className={styles.formCard}>
          <h3>{editingPetId ? '반려동물 정보 수정' : '반려동물 등록'}</h3>
          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.photoPicker}>
              <div className={styles.photoPreviewWrap}>
                <div className={styles.photoPreview}>
                  {photoPreview ? <img src={photoPreview} alt="" /> : <PawIcon />}
                </div>
                <label className={styles.photoPickBtn}>
                  <CameraIcon />
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    onChange={handlePhotoSelect}
                    hidden
                  />
                </label>
              </div>
              {photoPreview && (
                <button type="button" className={styles.photoClearLink} onClick={handlePhotoClear}>
                  사진 지우기
                </button>
              )}
            </div>

            <div className={styles.field}>
              <span className={styles.fieldLabel}>이름</span>
              <input
                type="text"
                placeholder="이름을 입력하세요"
                value={form.name}
                onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                maxLength={50}
                autoFocus
                required
              />
            </div>

            <div className={styles.field}>
              <span className={styles.fieldLabel}>종류</span>
              <div className={styles.chipRow}>
                {SPECIES_PRESETS.map((s) => (
                  <button
                    key={s}
                    type="button"
                    className={speciesChip === s ? styles.chipActive : styles.chip}
                    onClick={() => setSpeciesChip(s)}
                  >
                    {s}
                  </button>
                ))}
                <button
                  type="button"
                  className={speciesChip === '기타' ? styles.chipActive : styles.chip}
                  onClick={() => setSpeciesChip('기타')}
                >
                  기타
                </button>
              </div>
              {speciesChip === '기타' && (
                <input
                  type="text"
                  className={styles.customSpeciesInput}
                  placeholder="종류 직접 입력"
                  value={customSpecies}
                  onChange={(e) => setCustomSpecies(e.target.value)}
                  maxLength={30}
                  autoFocus
                />
              )}
            </div>

            <div className={styles.field}>
              <span className={styles.fieldLabel}>생년월일</span>
              <div className={styles.birthRow}>
                <input
                  type="date"
                  value={form.birthDate}
                  onChange={(e) => setForm((prev) => ({ ...prev, birthDate: e.target.value }))}
                  max={new Date().toISOString().slice(0, 10)}
                />
                {form.birthDate && <span className={styles.ageBadge}>{calcAge(form.birthDate)}</span>}
              </div>
            </div>

            <div className={styles.formActions}>
              <Button type="button" variant="secondary" onClick={closeForm}>
                취소
              </Button>
              <Button type="submit" disabled={saving}>
                {editingPetId ? '저장' : '등록'}
              </Button>
            </div>
          </form>
        </div>
      ) : (
        <button type="button" className={styles.addBtn} onClick={openCreateForm}>
          <PlusIcon /> 반려동물 등록
        </button>
      )}
    </div>
  );
}
