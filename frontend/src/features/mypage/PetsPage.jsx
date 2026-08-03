import { useEffect, useState } from 'react';
import { getMyPets, createPet, updatePet, deletePet, uploadPetPhoto, deletePetPhoto } from './api';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Modal from '../../components/Modal/Modal';
import Button from '../../components/Button/Button';
import { resolveImage } from '../../utils/resolveImage';
import styles from './PetsPage.module.scss';

const EMPTY_FORM = { name: '', species: '', birthDate: '' };

export default function PetsPage() {
  const { showToast } = useToast();
  const confirm = useConfirm();
  const [pets, setPets] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingPetId, setEditingPetId] = useState(null); // null이면 신규 등록
  const [form, setForm] = useState(EMPTY_FORM);

  async function load() {
    const res = await getMyPets();
    setPets(res.data.data);
  }

  useEffect(() => {
    load();
  }, []);

  function openCreateModal() {
    setEditingPetId(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
  }

  function openEditModal(pet) {
    setEditingPetId(pet.petId);
    setForm({ name: pet.name, species: pet.species ?? '', birthDate: pet.birthDate ?? '' });
    setModalOpen(true);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const payload = {
      name: form.name,
      species: form.species || null,
      birthDate: form.birthDate || null,
    };
    try {
      if (editingPetId) {
        await updatePet(editingPetId, payload);
      } else {
        await createPet(payload);
      }
      setModalOpen(false);
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '저장에 실패했습니다.', 'error');
    }
  }

  async function handleDelete(petId) {
    const ok = await confirm('반려동물 정보를 삭제하시겠습니까?');
    if (!ok) return;
    await deletePet(petId);
    load();
  }

  async function handlePhotoChange(petId, e) {
    const file = e.target.files[0];
    if (!file) return;
    await uploadPetPhoto(petId, file);
    e.target.value = '';
    load();
  }

  async function handlePhotoDelete(petId) {
    await deletePetPhoto(petId);
    load();
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <h1>반려동물</h1>
        <Button onClick={openCreateModal}>등록</Button>
      </div>

      <ul className={styles.list}>
        {pets.map((pet) => (
          <li key={pet.petId} className={styles.item}>
            <div className={styles.photo}>
              {pet.photoImg ? (
                <img src={resolveImage(pet.photoImg)} alt="" />
              ) : (
                <div className={styles.photoPlaceholder}>{pet.name[0]}</div>
              )}
            </div>
            <div className={styles.itemBody}>
              <span className={styles.name}>{pet.name}</span>
              <span className={styles.meta}>
                {pet.species ?? '종 미상'}
                {pet.birthDate && ` · ${pet.birthDate}`}
              </span>
              <div className={styles.photoActions}>
                <label className={styles.uploadLabel}>
                  사진 변경
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    onChange={(e) => handlePhotoChange(pet.petId, e)}
                    hidden
                  />
                </label>
                {pet.photoImg && <button onClick={() => handlePhotoDelete(pet.petId)}>삭제</button>}
              </div>
            </div>
            <div className={styles.actions}>
              <button onClick={() => openEditModal(pet)}>수정</button>
              <button onClick={() => handleDelete(pet.petId)}>삭제</button>
            </div>
          </li>
        ))}
        {pets.length === 0 && <p className={styles.empty}>등록된 반려동물이 없습니다.</p>}
      </ul>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)}>
        <h2>{editingPetId ? '반려동물 수정' : '반려동물 등록'}</h2>
        <form onSubmit={handleSubmit} className={styles.form}>
          <input
            type="text"
            placeholder="이름"
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            maxLength={50}
            required
          />
          <input
            type="text"
            placeholder="종류 (예: 강아지, 고양이)"
            value={form.species}
            onChange={(e) => setForm((prev) => ({ ...prev, species: e.target.value }))}
            maxLength={30}
          />
          <input
            type="date"
            value={form.birthDate}
            onChange={(e) => setForm((prev) => ({ ...prev, birthDate: e.target.value }))}
            max={new Date().toISOString().slice(0, 10)}
          />
          <div className={styles.modalActions}>
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>
              취소
            </Button>
            <Button type="submit">{editingPetId ? '수정' : '등록'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
